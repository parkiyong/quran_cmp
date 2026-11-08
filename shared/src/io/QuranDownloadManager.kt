package com.jetbrains.kmpapp.io

import com.jetbrains.kmpapp.data.source.PageProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okio.Path
import okio.buffer
import okio.use

import com.jetbrains.kmpapp.data.model.Qari
import okio.Path.Companion.toPath

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    EXTRACTING,
    COMPLETED,
    ERROR
}

data class DownloadProgressState(
    val isDownloading: Boolean = false,
    val stageTitle: String = "",
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val isExtracting: Boolean = false,
    val error: String? = null,
    val completed: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalBytes > 0L) {
            (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val status: DownloadStatus
        get() = when {
            error != null -> DownloadStatus.ERROR
            isExtracting -> DownloadStatus.EXTRACTING
            isDownloading -> DownloadStatus.DOWNLOADING
            completed -> DownloadStatus.COMPLETED
            else -> DownloadStatus.IDLE
        }

    val currentTaskTitle: String get() = stageTitle
    val fraction: Float get() = progressFraction
    val downloadedBytes: Long get() = bytesDownloaded
    val errorMessage: String? get() = error
}

class QuranDownloadManager(
    private val httpClient: HttpClient,
    private val fileManager: QuranFileManager
) {
    private val _mushafDownloadState = MutableStateFlow(DownloadProgressState())
    val mushafDownloadState: StateFlow<DownloadProgressState> = _mushafDownloadState.asStateFlow()
    val progress: StateFlow<DownloadProgressState> get() = mushafDownloadState

    suspend fun downloadFile(
        url: String,
        targetPath: Path,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        targetPath.parent?.let { fileManager.ensureDir(it) }
        val tmpPath = fileManager.tempDir / "${targetPath.name}.part"
        if (fileManager.fileSystem.exists(tmpPath)) {
            fileManager.fileSystem.delete(tmpPath)
        }

        httpClient.prepareGet(url).execute { response ->
            if (!response.status.isSuccess()) {
                throw IllegalStateException("HTTP ${response.status.value} when downloading $url")
            }
            val total = response.contentLength() ?: -1L
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(64 * 1024)
            var downloaded = 0L

            fileManager.fileSystem.sink(tmpPath).buffer().use { sink ->
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    sink.write(buffer, 0, read)
                    downloaded += read
                    onProgress(downloaded, total)
                }
                sink.flush()
            }
        }

        if (fileManager.fileSystem.exists(targetPath)) {
            fileManager.fileSystem.delete(targetPath)
        }
        fileManager.fileSystem.atomicMove(tmpPath, targetPath)
    }

    suspend fun downloadAndExtractZip(
        url: String,
        destinationDir: Path,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> },
        onExtracting: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val zipTemp = fileManager.tempDir / "archive_${url.hashCode()}.zip"
        try {
            downloadFile(url, zipTemp, onProgress)
            onExtracting()
            fileManager.extractZip(zipTemp, destinationDir)
        } finally {
            if (fileManager.fileSystem.exists(zipTemp)) {
                fileManager.fileSystem.delete(zipTemp)
            }
        }
    }

    suspend fun downloadSinglePageIfMissing(provider: PageProvider, page: Int): Path =
        withContext(Dispatchers.IO) {
            val path = fileManager.resolvePageImagePath(provider, page)
            if (!fileManager.hasPageImage(provider, page)) {
                downloadFile(provider.pageImageUrl(page), path)
            }
            path
        }

    suspend fun downloadFullMushafBundle(provider: PageProvider): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val needImages = !fileManager.hasAllPageImages(provider)
                val needAyahInfo = !fileManager.hasAyahInfoDatabase(provider)
                val needArabicDb = !fileManager.hasArabicDatabase(provider)

                val totalSteps = listOf(needImages, needAyahInfo, needArabicDb).count { it }.coerceAtLeast(1)
                var currentStep = 0

                if (needImages) {
                    currentStep++
                    _mushafDownloadState.value = DownloadProgressState(
                        isDownloading = true,
                        stageTitle = "Downloading ${provider.mushafType.defaultTitle} Page Images...",
                        stepIndex = currentStep,
                        totalSteps = totalSteps
                    )
                    downloadAndExtractZip(
                        url = provider.zipFileUrl,
                        destinationDir = fileManager.getMushafImagesDir(provider),
                        onProgress = { read, total ->
                            _mushafDownloadState.value = _mushafDownloadState.value.copy(
                                bytesDownloaded = read,
                                totalBytes = total
                            )
                        },
                        onExtracting = {
                            _mushafDownloadState.value = _mushafDownloadState.value.copy(
                                isExtracting = true,
                                stageTitle = "Extracting ${provider.mushafType.defaultTitle} Pages..."
                            )
                        }
                    )
                }

                if (needAyahInfo) {
                    currentStep++
                    _mushafDownloadState.value = DownloadProgressState(
                        isDownloading = true,
                        stageTitle = "Downloading Ayah Coordinate Database...",
                        stepIndex = currentStep,
                        totalSteps = totalSteps
                    )
                    downloadAndExtractZip(
                        url = provider.ayahInfoZipUrl,
                        destinationDir = fileManager.getMushafDatabaseDir(provider),
                        onProgress = { read, total ->
                            _mushafDownloadState.value = _mushafDownloadState.value.copy(
                                bytesDownloaded = read,
                                totalBytes = total
                            )
                        },
                        onExtracting = {
                            _mushafDownloadState.value = _mushafDownloadState.value.copy(
                                isExtracting = true,
                                stageTitle = "Extracting Ayah Coordinate Database..."
                            )
                        }
                    )
                }

                if (needArabicDb) {
                    currentStep++
                    _mushafDownloadState.value = DownloadProgressState(
                        isDownloading = true,
                        stageTitle = "Downloading Arabic Uthmani Text Database...",
                        stepIndex = currentStep,
                        totalSteps = totalSteps
                    )
                    try {
                        downloadAndExtractZip(
                            url = provider.arabicDatabaseZipUrl,
                            destinationDir = fileManager.databasesDir,
                            onProgress = { read, total ->
                                _mushafDownloadState.value = _mushafDownloadState.value.copy(
                                    bytesDownloaded = read,
                                    totalBytes = total
                                )
                            },
                            onExtracting = {
                                _mushafDownloadState.value = _mushafDownloadState.value.copy(
                                    isExtracting = true,
                                    stageTitle = "Extracting Arabic Text Database..."
                                )
                            }
                        )
                    } catch (_: Exception) {
                        // Fallback to standard Madani Uthmani DB if flavor-specific Arabic DB zip is unavailable
                        val fallbackUrl = "https://android.quran.com/data/databases/quran.ar.uthmani.v2.db.zip"
                        downloadAndExtractZip(
                            url = fallbackUrl,
                            destinationDir = fileManager.databasesDir
                        )
                        val extracted = fileManager.databasesDir / "quran.ar.uthmani.v2.db"
                        val expected = fileManager.getArabicDatabasePath(provider)
                        if (fileManager.fileSystem.exists(extracted) && !fileManager.fileSystem.exists(expected)) {
                            fileManager.fileSystem.copy(extracted, expected)
                        }
                    }
                }

                _mushafDownloadState.value = DownloadProgressState(
                    isDownloading = false,
                    stageTitle = "Ready",
                    completed = true
                )
                true
            } catch (e: Exception) {
                _mushafDownloadState.value = DownloadProgressState(
                    isDownloading = false,
                    error = e.message ?: "Download failed. Please check your internet connection."
                )
                false
            }
        }

    fun resetMushafDownloadState() {
        _mushafDownloadState.value = DownloadProgressState()
    }

    suspend fun downloadAndInstallMushafBundle(provider: PageProvider): Boolean =
        downloadFullMushafBundle(provider)

    suspend fun downloadFile(url: String, targetPathStr: String): Boolean {
        return try {
            downloadFile(url, targetPathStr.toPath())
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun downloadTranslation(fileUrl: String, fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (fileUrl.endsWith(".zip", ignoreCase = true)) {
                    downloadAndExtractZip(fileUrl, fileManager.databasesDir)
                } else {
                    downloadFile(fileUrl, fileManager.getTranslationDatabasePath(fileName))
                }
                true
            } catch (_: Exception) {
                false
            }
        }

    suspend fun downloadQariTimingDatabase(qari: Qari): Boolean =
        withContext(Dispatchers.IO) {
            val dbBase = qari.db ?: return@withContext false
            val zipUrl = "https://android.quran.com/data/databases/audio/$dbBase.db.zip"
            try {
                downloadAndExtractZip(zipUrl, fileManager.audioDatabasesDir)
                true
            } catch (_: Exception) {
                false
            }
        }

    suspend fun downloadGaplessSuraAudio(qari: Qari, sura: Int): Boolean =
        withContext(Dispatchers.IO) {
            val suraPadded = sura.toString().padStart(3, '0')
            val url = "${qari.url}$suraPadded.mp3"
            val dest = fileManager.getGaplessSuraAudioPath(qari, sura)
            try {
                downloadFile(url, dest)
                true
            } catch (_: Exception) {
                false
            }
        }
}

