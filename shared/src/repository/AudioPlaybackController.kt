package com.jetbrains.kmpapp.repository

import com.jetbrains.kmpapp.data.core.QuranInfo
import com.jetbrains.kmpapp.data.model.Qari
import com.jetbrains.kmpapp.data.model.SuraAyah
import com.jetbrains.kmpapp.data.source.DefaultQariCatalog
import com.jetbrains.kmpapp.database.AyahTimingEntry
import com.jetbrains.kmpapp.database.SuraTimingDatabaseHandler
import com.jetbrains.kmpapp.io.QuranDownloadManager
import com.jetbrains.kmpapp.io.QuranFileManager
import com.jetbrains.kmpapp.platform.PlatformAudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val selectedQari: Qari = DefaultQariCatalog.qaris.first(),
    val currentVerse: SuraAyah? = null,
    val startVerse: SuraAyah? = null,
    val endVerse: SuraAyah? = null,
    val playbackSpeed: Float = 1.0f,
    val verseRepeatCount: Int = 0, // 0 = no repeat, 1..3, -1 = infinite
    val rangeRepeatCount: Int = 0,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L
)

class AudioPlaybackController(
    private val fileManager: QuranFileManager,
    private val downloadManager: QuranDownloadManager,
    private val settingsRepository: QuranSettingsRepository,
    private val quranInfo: QuranInfo
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val player = PlatformAudioPlayer()
    private val timingCache = mutableMapOf<String, SuraTimingDatabaseHandler>()

    private val _state = MutableStateFlow(
        AudioPlaybackState(
            selectedQari = DefaultQariCatalog.qaris.find {
                it.id == settingsRepository.settings.value.selectedQariId
            } ?: DefaultQariCatalog.qaris.first(),
            playbackSpeed = settingsRepository.settings.value.audioPlaybackSpeed,
            verseRepeatCount = settingsRepository.settings.value.audioRepeatVerseCount,
            rangeRepeatCount = settingsRepository.settings.value.audioRepeatRangeCount
        )
    )
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private var trackerJob: Job? = null
    private var activeSurahInPlayer: Int = -1
    private var remainingVerseRepeats: Int = 0
    private var remainingRangeRepeats: Int = 0

    var onActiveVerseChanged: ((SuraAyah, Int) -> Unit)? = null

    fun getAvailableQaris(): List<Qari> = DefaultQariCatalog.qaris

    fun selectQari(qari: Qari) {
        val wasPlaying = _state.value.isPlaying
        val currentVerse = _state.value.currentVerse
        stop()
        settingsRepository.setSelectedQariId(qari.id)
        _state.update { it.copy(selectedQari = qari) }
        if (wasPlaying && currentVerse != null) {
            playFrom(
                start = currentVerse,
                end = _state.value.endVerse ?: SuraAyah(currentVerse.sura, quranInfo.getNumberOfAyahs(currentVerse.sura))
            )
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        settingsRepository.setAudioPlaybackSpeed(clamped)
        player.setPlaybackSpeed(clamped)
        _state.update { it.copy(playbackSpeed = clamped) }
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val current = _state.value.playbackSpeed
        val nextIndex = (speeds.indexOfFirst { kotlin.math.abs(it - current) < 0.05f } + 1) % speeds.size
        setPlaybackSpeed(speeds[nextIndex])
    }

    fun cycleVerseRepeat() {
        val options = listOf(0, 1, 2, 3, -1)
        val current = _state.value.verseRepeatCount
        val next = options[(options.indexOf(current) + 1) % options.size]
        remainingVerseRepeats = next
        settingsRepository.setAudioRepeat(next, _state.value.rangeRepeatCount)
        _state.update { it.copy(verseRepeatCount = next) }
    }

    fun cycleRangeRepeat() {
        val options = listOf(0, 1, 2, 3, -1)
        val current = _state.value.rangeRepeatCount
        val next = options[(options.indexOf(current) + 1) % options.size]
        remainingRangeRepeats = next
        settingsRepository.setAudioRepeat(_state.value.verseRepeatCount, next)
        _state.update { it.copy(rangeRepeatCount = next) }
    }

    fun playPage(page: Int) {
        val bounds = quranInfo.getPageBounds(page)
        val start = SuraAyah(bounds[0], bounds[1])
        val end = SuraAyah(bounds[2], bounds[3])
        playFrom(start, end)
    }

    fun playFrom(
        start: SuraAyah,
        end: SuraAyah = SuraAyah(start.sura, quranInfo.getNumberOfAyahs(start.sura))
    ) {
        remainingVerseRepeats = _state.value.verseRepeatCount
        remainingRangeRepeats = _state.value.rangeRepeatCount
        _state.update {
            it.copy(
                startVerse = start,
                endVerse = end,
                currentVerse = start,
                isLoading = true,
                statusMessage = "Preparing recitation..."
            )
        }
        scope.launch {
            startPlaybackForVerse(start)
        }
    }

    fun togglePlayPause() {
        val current = _state.value
        if (current.isPlaying) {
            player.pause()
            _state.update { it.copy(isPlaying = false, isPaused = true) }
        } else if (current.isPaused && current.currentVerse != null) {
            player.resume()
            _state.update { it.copy(isPlaying = true, isPaused = false) }
        } else if (current.currentVerse != null) {
            playFrom(
                current.currentVerse,
                current.endVerse ?: SuraAyah(current.currentVerse.sura, quranInfo.getNumberOfAyahs(current.currentVerse.sura))
            )
        }
    }

    fun nextAyah() {
        val current = _state.value.currentVerse ?: return
        val next = getNextVerse(current) ?: return
        remainingVerseRepeats = _state.value.verseRepeatCount
        scope.launch {
            startPlaybackForVerse(next)
        }
    }

    fun previousAyah() {
        val current = _state.value.currentVerse ?: return
        val prev = getPreviousVerse(current) ?: return
        remainingVerseRepeats = _state.value.verseRepeatCount
        scope.launch {
            startPlaybackForVerse(prev)
        }
    }

    fun stop() {
        trackerJob?.cancel()
        trackerJob = null
        activeSurahInPlayer = -1
        player.stop()
        _state.update {
            it.copy(
                isPlaying = false,
                isPaused = false,
                isLoading = false,
                statusMessage = null,
                currentVerse = null,
                currentPositionMs = 0L,
                durationMs = 0L
            )
        }
    }

    private suspend fun startPlaybackForVerse(verse: SuraAyah) {
        val qari = _state.value.selectedQari
        _state.update {
            it.copy(
                currentVerse = verse,
                isLoading = true,
                statusMessage = "Loading ${qari.name}..."
            )
        }
        notifyVerseChanged(verse)

        if (qari.isGapless) {
            startGaplessPlayback(qari, verse)
        } else {
            startGappedPlayback(qari, verse)
        }
    }

    private suspend fun startGaplessPlayback(qari: Qari, verse: SuraAyah) {
        val dbName = qari.db ?: qari.path
        val dbPath = fileManager.getGaplessTimingDbPath(dbName)
        if (!fileManager.fileExists(dbPath)) {
            _state.update { it.copy(statusMessage = "Downloading timing DB for ${qari.name}...") }
            downloadManager.downloadQariTimingDatabase(qari)
        }

        val timingHandler = getTimingHandler(dbPath)
        val timings = timingHandler?.getAyahTimingsForSura(verse.sura).orEmpty()
        val seekMs = timingHandler?.findStartTimeMsForAyah(timings, verse.ayah) ?: 0L

        if (activeSurahInPlayer == verse.sura && (player.isPlaying() || _state.value.isPaused)) {
            player.seekTo(seekMs)
            if (!player.isPlaying()) {
                player.resume()
            }
            _state.update {
                it.copy(
                    isPlaying = true,
                    isPaused = false,
                    isLoading = false,
                    statusMessage = null
                )
            }
            startGaplessPositionTracker(qari, verse.sura, timings)
            return
        }

        val localMp3 = fileManager.getGaplessSuraAudioPath(qari.path, verse.sura)
        val source = if (fileManager.fileExists(localMp3)) {
            localMp3
        } else {
            val suraPadded = verse.sura.toString().padStart(3, '0')
            "${qari.url}$suraPadded.mp3"
        }

        activeSurahInPlayer = verse.sura
        player.play(
            fileOrUrl = source,
            startPositionMs = seekMs,
            speed = _state.value.playbackSpeed,
            onCompletion = {
                scope.launch { handleTrackCompletion() }
            },
            onError = { err ->
                scope.launch {
                    _state.update {
                        it.copy(isPlaying = false, isLoading = false, statusMessage = err)
                    }
                }
            }
        )
        _state.update {
            it.copy(
                isPlaying = true,
                isPaused = false,
                isLoading = false,
                statusMessage = null
            )
        }
        startGaplessPositionTracker(qari, verse.sura, timings)
    }

    private fun startGaplessPositionTracker(
        qari: Qari,
        sura: Int,
        timings: List<AyahTimingEntry>
    ) {
        trackerJob?.cancel()
        trackerJob = scope.launch {
            val timingHandler = getTimingHandler(fileManager.getGaplessTimingDbPath(qari.db ?: qari.path))
            while (isActive) {
                delay(120)
                if (!player.isPlaying()) continue

                val pos = player.getCurrentPositionMs()
                _state.update { it.copy(currentPositionMs = pos) }

                if (timings.isNotEmpty() && timingHandler != null) {
                    val detectedAyah = timingHandler.findAyahAtTimeMs(timings, pos)
                    val current = _state.value.currentVerse
                    if (detectedAyah != null && current != null && detectedAyah != current.ayah && detectedAyah > 0) {
                        if (remainingVerseRepeats != 0) {
                            if (remainingVerseRepeats > 0) remainingVerseRepeats--
                            val repeatSeek = timingHandler.findStartTimeMsForAyah(timings, current.ayah)
                            player.seekTo(repeatSeek)
                            continue
                        }

                        val newVerse = SuraAyah(sura, detectedAyah)
                        val endVerse = _state.value.endVerse
                        if (endVerse != null && newVerse > endVerse) {
                            if (remainingRangeRepeats != 0) {
                                if (remainingRangeRepeats > 0) remainingRangeRepeats--
                                val startVerse = _state.value.startVerse ?: newVerse
                                remainingVerseRepeats = _state.value.verseRepeatCount
                                startPlaybackForVerse(startVerse)
                            } else {
                                stop()
                            }
                            break
                        }

                        remainingVerseRepeats = _state.value.verseRepeatCount
                        _state.update { it.copy(currentVerse = newVerse) }
                        notifyVerseChanged(newVerse)
                    }
                }
            }
        }
    }

    private suspend fun startGappedPlayback(qari: Qari, verse: SuraAyah) {
        trackerJob?.cancel()
        activeSurahInPlayer = -1

        val localFile = fileManager.getGappedAyahAudioPath(qari.path, verse.sura, verse.ayah)
        val source = if (fileManager.fileExists(localFile)) {
            localFile
        } else {
            val sPad = verse.sura.toString().padStart(3, '0')
            val aPad = verse.ayah.toString().padStart(3, '0')
            "${qari.url}$sPad$aPad.mp3"
        }

        player.play(
            fileOrUrl = source,
            startPositionMs = 0L,
            speed = _state.value.playbackSpeed,
            onCompletion = {
                scope.launch { handleTrackCompletion() }
            },
            onError = { err ->
                scope.launch {
                    _state.update {
                        it.copy(isPlaying = false, isLoading = false, statusMessage = err)
                    }
                }
            }
        )
        _state.update {
            it.copy(
                isPlaying = true,
                isPaused = false,
                isLoading = false,
                statusMessage = null
            )
        }

        trackerJob = scope.launch {
            while (isActive) {
                delay(150)
                if (player.isPlaying()) {
                    _state.update {
                        it.copy(currentPositionMs = player.getCurrentPositionMs())
                    }
                }
            }
        }
    }

    private suspend fun handleTrackCompletion() {
        val currentState = _state.value
        val currentVerse = currentState.currentVerse ?: run {
            stop()
            return
        }

        if (currentState.selectedQari.isGapless) {
            val endVerse = currentState.endVerse
            val nextSura = currentVerse.sura + 1
            if (endVerse != null && nextSura <= endVerse.sura && nextSura <= 114) {
                startPlaybackForVerse(SuraAyah(nextSura, 1))
            } else if (remainingRangeRepeats != 0) {
                if (remainingRangeRepeats > 0) remainingRangeRepeats--
                val start = currentState.startVerse ?: SuraAyah(currentVerse.sura, 1)
                startPlaybackForVerse(start)
            } else {
                stop()
            }
        } else {
            if (remainingVerseRepeats != 0) {
                if (remainingVerseRepeats > 0) remainingVerseRepeats--
                startPlaybackForVerse(currentVerse)
                return
            }

            remainingVerseRepeats = currentState.verseRepeatCount
            val nextVerse = getNextVerse(currentVerse)
            val endVerse = currentState.endVerse
            if (nextVerse != null && (endVerse == null || nextVerse <= endVerse)) {
                startPlaybackForVerse(nextVerse)
            } else if (remainingRangeRepeats != 0) {
                if (remainingRangeRepeats > 0) remainingRangeRepeats--
                val start = currentState.startVerse ?: currentVerse
                startPlaybackForVerse(start)
            } else {
                stop()
            }
        }
    }

    private fun notifyVerseChanged(verse: SuraAyah) {
        val page = quranInfo.getPageFromSuraAyah(verse.sura, verse.ayah)
        if (page > 0) {
            onActiveVerseChanged?.invoke(verse, page)
        }
    }

    private fun getNextVerse(current: SuraAyah): SuraAyah? {
        val maxAyah = quranInfo.getNumberOfAyahs(current.sura)
        return if (current.ayah < maxAyah) {
            SuraAyah(current.sura, current.ayah + 1)
        } else if (current.sura < 114) {
            SuraAyah(current.sura + 1, 1)
        } else {
            null
        }
    }

    private fun getPreviousVerse(current: SuraAyah): SuraAyah? {
        return if (current.ayah > 1) {
            SuraAyah(current.sura, current.ayah - 1)
        } else if (current.sura > 1) {
            val prevSura = current.sura - 1
            SuraAyah(prevSura, quranInfo.getNumberOfAyahs(prevSura))
        } else {
            null
        }
    }

    private fun getTimingHandler(dbPath: String): SuraTimingDatabaseHandler? {
        if (!fileManager.fileExists(dbPath)) return null
        return timingCache.getOrPut(dbPath) {
            SuraTimingDatabaseHandler(dbPath)
        }
    }

    fun isSuraDownloaded(qari: Qari, sura: Int): Boolean {
        return if (qari.isGapless) {
            fileManager.fileExists(fileManager.getGaplessSuraAudioPath(qari.path, sura))
        } else {
            val totalAyahs = quranInfo.getNumberOfAyahs(sura)
            totalAyahs > 0 &&
                fileManager.fileExists(fileManager.getGappedAyahAudioPath(qari.path, sura, 1)) &&
                fileManager.fileExists(fileManager.getGappedAyahAudioPath(qari.path, sura, totalAyahs))
        }
    }

    suspend fun downloadSuraAudio(
        qari: Qari,
        sura: Int,
        onProgress: (Float) -> Unit = {}
    ): Boolean {
        if (qari.isGapless) {
            downloadManager.downloadQariTimingDatabase(qari)
            onProgress(0.2f)
            val ok = downloadManager.downloadGaplessSuraAudio(qari, sura)
            onProgress(1.0f)
            return ok
        } else {
            val totalAyahs = quranInfo.getNumberOfAyahs(sura)
            var allOk = true
            for (ayah in 1..totalAyahs) {
                val dest = fileManager.getGappedAyahAudioPath(qari.path, sura, ayah)
                if (!fileManager.fileExists(dest)) {
                    val sPad = sura.toString().padStart(3, '0')
                    val aPad = ayah.toString().padStart(3, '0')
                    val url = "${qari.url}$sPad$aPad.mp3"
                    val ok = downloadManager.downloadFile(url, dest)
                    if (!ok) allOk = false
                }
                onProgress(ayah.toFloat() / totalAyahs.toFloat())
            }
            return allOk
        }
    }

    fun deleteSuraAudio(qari: Qari, sura: Int) {
        if (qari.isGapless) {
            fileManager.deletePath(fileManager.getGaplessSuraAudioPath(qari.path, sura))
        } else {
            val totalAyahs = quranInfo.getNumberOfAyahs(sura)
            for (ayah in 1..totalAyahs) {
                fileManager.deletePath(fileManager.getGappedAyahAudioPath(qari.path, sura, ayah))
            }
        }
    }
}
