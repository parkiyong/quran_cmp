package com.jetbrains.kmpapp.repository

import com.jetbrains.kmpapp.data.model.MushafType
import com.jetbrains.kmpapp.data.model.PageAppearance
import com.jetbrains.kmpapp.data.model.ReaderViewMode
import com.jetbrains.kmpapp.io.QuranFileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath

@Serializable
data class QuranAppSettings(
    val mushafType: MushafType = MushafType.MADANI,
    val pageAppearance: PageAppearance = PageAppearance.LIGHT_CREAM,
    val nightModeTextBrightness: Int = 255, // 50..255
    val nightModeBackgroundBrightness: Int = 0, // 0..150
    val readerViewMode: ReaderViewMode = ReaderViewMode.MUSHAF_PAGE,
    val isDualPageEnabled: Boolean = false,
    val showPageInfoOverlay: Boolean = true,
    val showSidelines: Boolean = false,
    val arabicFontSize: Float = 28f,
    val translationFontSize: Float = 16f,
    val showArabicInTranslation: Boolean = true,
    val activeTranslationFileNames: List<String> = emptyList(),
    val selectedQariId: Int = 0,
    val audioPlaybackSpeed: Float = 1.0f,
    val audioRepeatVerseCount: Int = 0, // 0 = no repeat, 1 = 1x, 2 = 2x, 3 = 3x, -1 = loop
    val audioRepeatRangeCount: Int = 0,
    val lastPageByMushaf: Map<String, Int> = emptyMap(),
    val showSuraTranslatedName: Boolean = true,
    val useArabicNames: Boolean = false
)

class QuranSettingsRepository(
    private val fileManager: QuranFileManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val settingsPath = "${fileManager.getAppRootDirectory()}/quran_settings.json".toPath()

    private val _settings = MutableStateFlow(loadFromDisk())
    val settings: StateFlow<QuranAppSettings> = _settings.asStateFlow()

    private fun loadFromDisk(): QuranAppSettings {
        return try {
            if (fileManager.fileSystem.exists(settingsPath)) {
                val content = fileManager.fileSystem.read(settingsPath) { readUtf8() }
                json.decodeFromString(QuranAppSettings.serializer(), content)
            } else {
                QuranAppSettings()
            }
        } catch (e: Exception) {
            QuranAppSettings()
        }
    }

    private fun persist(newSettings: QuranAppSettings) {
        try {
            val parent = settingsPath.parent
            if (parent != null && !fileManager.fileSystem.exists(parent)) {
                fileManager.fileSystem.createDirectories(parent)
            }
            val encoded = json.encodeToString(QuranAppSettings.serializer(), newSettings)
            fileManager.fileSystem.write(settingsPath) {
                writeUtf8(encoded)
            }
        } catch (_: Exception) {
        }
    }

    fun updateSettings(transform: (QuranAppSettings) -> QuranAppSettings) {
        _settings.update { current ->
            val updated = transform(current)
            persist(updated)
            updated
        }
    }

    fun setMushafType(type: MushafType) = updateSettings { it.copy(mushafType = type) }

    fun setPageAppearance(appearance: PageAppearance) = updateSettings { it.copy(pageAppearance = appearance) }

    fun setNightModeBrightness(textBrightness: Int, bgBrightness: Int) = updateSettings {
        it.copy(
            nightModeTextBrightness = textBrightness.coerceIn(50, 255),
            nightModeBackgroundBrightness = bgBrightness.coerceIn(0, 150)
        )
    }

    fun setReaderViewMode(mode: ReaderViewMode) = updateSettings { it.copy(readerViewMode = mode) }

    fun setDualPageEnabled(enabled: Boolean) = updateSettings { it.copy(isDualPageEnabled = enabled) }

    fun setShowPageInfoOverlay(show: Boolean) = updateSettings { it.copy(showPageInfoOverlay = show) }

    fun setFontSizes(arabicSize: Float, translationSize: Float) = updateSettings {
        it.copy(
            arabicFontSize = arabicSize.coerceIn(18f, 48f),
            translationFontSize = translationSize.coerceIn(12f, 32f)
        )
    }

    fun setShowArabicInTranslation(show: Boolean) = updateSettings {
        it.copy(showArabicInTranslation = show)
    }

    fun setActiveTranslations(fileNames: List<String>) = updateSettings {
        it.copy(activeTranslationFileNames = fileNames.distinct())
    }

    fun toggleActiveTranslation(fileName: String) = updateSettings { current ->
        val currentList = current.activeTranslationFileNames.toMutableList()
        if (currentList.contains(fileName)) {
            currentList.remove(fileName)
        } else {
            currentList.add(fileName)
        }
        current.copy(activeTranslationFileNames = currentList)
    }

    fun moveActiveTranslation(fileName: String, direction: Int) = updateSettings { current ->
        val currentList = current.activeTranslationFileNames.toMutableList()
        val idx = currentList.indexOf(fileName)
        val target = idx + direction
        if (idx in currentList.indices && target in currentList.indices) {
            val item = currentList.removeAt(idx)
            currentList.add(target, item)
        }
        current.copy(activeTranslationFileNames = currentList)
    }

    fun setSelectedQariId(qariId: Int) = updateSettings { it.copy(selectedQariId = qariId) }

    fun setAudioPlaybackSpeed(speed: Float) = updateSettings { it.copy(audioPlaybackSpeed = speed) }

    fun setAudioRepeat(verseRepeat: Int, rangeRepeat: Int) = updateSettings {
        it.copy(audioRepeatVerseCount = verseRepeat, audioRepeatRangeCount = rangeRepeat)
    }

    fun setLastPage(mushafType: MushafType, page: Int) = updateSettings { current ->
        val map = current.lastPageByMushaf.toMutableMap()
        map[mushafType.id] = page
        current.copy(lastPageByMushaf = map)
    }

    fun getLastPage(mushafType: MushafType): Int {
        return _settings.value.lastPageByMushaf[mushafType.id] ?: 1
    }
}
