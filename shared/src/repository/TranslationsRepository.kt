package com.jetbrains.kmpapp.repository

import com.jetbrains.kmpapp.data.model.Translation
import com.jetbrains.kmpapp.data.model.TranslationItem
import com.jetbrains.kmpapp.data.model.TranslationList
import com.jetbrains.kmpapp.io.DownloadStatus
import com.jetbrains.kmpapp.io.QuranDownloadManager
import com.jetbrains.kmpapp.io.QuranFileManager
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath

class TranslationsRepository(
    private val httpClient: HttpClient,
    private val fileManager: QuranFileManager,
    private val downloadManager: QuranDownloadManager,
    private val settingsRepository: QuranSettingsRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val cachedListPath = "${fileManager.getAppRootDirectory()}/cached_translations_v5.json".toPath()

    private val _translations = MutableStateFlow<List<TranslationItem>>(emptyList())
    val translations: StateFlow<List<TranslationItem>> = _translations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _downloadingIds = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadingIds: StateFlow<Map<Int, Float>> = _downloadingIds.asStateFlow()

    init {
        loadCachedCatalog()
    }

    private fun loadCachedCatalog() {
        try {
            if (fileManager.fileSystem.exists(cachedListPath)) {
                val raw = fileManager.fileSystem.read(cachedListPath) { readUtf8() }
                val parsed = json.decodeFromString(TranslationList.serializer(), raw)
                rebuildTranslationItems(parsed.translations)
            }
        } catch (_: Exception) {
        }
    }

    suspend fun refreshTranslationsCatalog(
        endpoint: String = "https://quran.app/data/translations.php?v=5"
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        try {
            val responseText = httpClient.get(endpoint).bodyAsText()
            val parsed = json.decodeFromString(TranslationList.serializer(), responseText)
            fileManager.fileSystem.write(cachedListPath) {
                writeUtf8(responseText)
            }
            rebuildTranslationItems(parsed.translations)
        } catch (e: Exception) {
            if (_translations.value.isEmpty()) {
                _errorMessage.value = e.message ?: "Failed to load translations catalog"
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun syncInstalledStatus() {
        val current = _translations.value.map { it.translation }
        if (current.isNotEmpty()) {
            rebuildTranslationItems(current)
        }
    }

    private fun rebuildTranslationItems(rawList: List<Translation>) {
        val activeNames = settingsRepository.settings.value.activeTranslationFileNames
        val items = rawList.map { tr ->
            val dbPath = fileManager.getDatabaseFilePath(tr.fileName)
            val isDownloaded = fileManager.fileExists(dbPath)
            val orderIndex = activeNames.indexOf(tr.fileName)
            TranslationItem(
                translation = tr,
                localVersion = if (isDownloaded) tr.currentVersion else 0,
                displayOrder = if (orderIndex >= 0) orderIndex else Int.MAX_VALUE,
                isActive = isDownloaded && orderIndex >= 0
            )
        }.sortedWith(
            compareByDescending<TranslationItem> { it.isDownloaded }
                .thenBy { it.displayOrder }
                .thenBy { it.translation.languageCode ?: "zz" }
                .thenBy { it.translation.displayName }
        )
        _translations.value = items
    }

    suspend fun downloadTranslation(translation: Translation): Boolean {
        _downloadingIds.value = _downloadingIds.value + (translation.id to 0f)
        return try {
            val success = downloadManager.downloadTranslation(translation.fileUrl, translation.fileName)
            if (success) {
                // Auto-enable translation when downloaded
                val currentActive = settingsRepository.settings.value.activeTranslationFileNames
                if (!currentActive.contains(translation.fileName)) {
                    settingsRepository.setActiveTranslations(currentActive + translation.fileName)
                }
                syncInstalledStatus()
            }
            success
        } finally {
            _downloadingIds.value = _downloadingIds.value - translation.id
        }
    }

    fun deleteTranslation(translation: Translation) {
        val dbPath = fileManager.getDatabaseFilePath(translation.fileName)
        fileManager.deletePath(dbPath)
        val currentActive = settingsRepository.settings.value.activeTranslationFileNames
        if (currentActive.contains(translation.fileName)) {
            settingsRepository.setActiveTranslations(currentActive - translation.fileName)
        }
        syncInstalledStatus()
    }

    fun toggleTranslationActive(translation: Translation) {
        settingsRepository.toggleActiveTranslation(translation.fileName)
        syncInstalledStatus()
    }

    fun moveTranslationOrder(translation: Translation, direction: Int) {
        settingsRepository.moveActiveTranslation(translation.fileName, direction)
        syncInstalledStatus()
    }
}
