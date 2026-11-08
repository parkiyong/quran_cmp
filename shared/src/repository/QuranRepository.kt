package com.jetbrains.kmpapp.repository

import com.jetbrains.kmpapp.data.core.QuranDisplayData
import com.jetbrains.kmpapp.data.core.QuranInfo
import com.jetbrains.kmpapp.data.model.AyahCoordinates
import com.jetbrains.kmpapp.data.model.MushafType
import com.jetbrains.kmpapp.data.model.PageCoordinates
import com.jetbrains.kmpapp.data.model.QuranText
import com.jetbrains.kmpapp.data.model.SearchResultItem
import com.jetbrains.kmpapp.data.model.VerseRange
import com.jetbrains.kmpapp.data.source.PageProvider
import com.jetbrains.kmpapp.data.source.PageProviderRegistry
import com.jetbrains.kmpapp.database.AyahInfoDatabaseHandler
import com.jetbrains.kmpapp.database.BookmarksDatabaseHandler
import com.jetbrains.kmpapp.database.QuranTextDatabaseHandler
import com.jetbrains.kmpapp.io.QuranDownloadManager
import com.jetbrains.kmpapp.io.QuranFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

data class VerseWithTranslations(
    val sura: Int,
    val ayah: Int,
    val arabicText: String,
    val translations: List<Pair<String, QuranText>>
)

class QuranRepository(
    val fileManager: QuranFileManager,
    val downloadManager: QuranDownloadManager,
    val settingsRepository: QuranSettingsRepository,
    val translationsRepository: TranslationsRepository,
    val bookmarksHandler: BookmarksDatabaseHandler
) {
    private val ayahInfoCache = mutableMapOf<String, AyahInfoDatabaseHandler>()
    private val textDbCache = mutableMapOf<String, QuranTextDatabaseHandler>()

    fun getCurrentMushafType(): MushafType = settingsRepository.settings.value.mushafType

    fun getPageProvider(type: MushafType = getCurrentMushafType()): PageProvider =
        PageProviderRegistry.getProvider(type)

    fun getQuranInfo(type: MushafType = getCurrentMushafType()): QuranInfo =
        QuranInfo(getPageProvider(type).dataSource)

    fun isMushafBundleReady(type: MushafType = getCurrentMushafType()): Boolean {
        val provider = getPageProvider(type)
        val totalPages = provider.dataSource.numberOfPages
        return fileManager.isMushafBundleComplete(provider, totalPages)
    }

    fun getPageImageFilePath(page: Int, type: MushafType = getCurrentMushafType()): String {
        val provider = getPageProvider(type)
        return fileManager.getPageImagePath(provider, page)
    }

    private fun getAyahInfoHandler(type: MushafType = getCurrentMushafType()): AyahInfoDatabaseHandler? {
        val provider = getPageProvider(type)
        val path = fileManager.getAyahInfoDbFilePath(provider)
        if (!fileManager.fileExists(path)) return null
        return ayahInfoCache.getOrPut(path) {
            AyahInfoDatabaseHandler(path)
        }
    }

    private fun getTextDbHandler(fileName: String): QuranTextDatabaseHandler? {
        val path = fileManager.getDatabaseFilePath(fileName)
        if (!fileManager.fileExists(path)) return null
        return textDbCache.getOrPut(path) {
            QuranTextDatabaseHandler(path)
        }
    }

    suspend fun getPageCoordinates(page: Int, type: MushafType = getCurrentMushafType()): PageCoordinates? =
        withContext(Dispatchers.IO) {
            getAyahInfoHandler(type)?.getPageCoordinates(page)
        }

    suspend fun getAyahCoordinates(page: Int, type: MushafType = getCurrentMushafType()): AyahCoordinates? =
        withContext(Dispatchers.IO) {
            getAyahInfoHandler(type)?.getAyahCoordinates(page)
        }

    suspend fun getVersesForPage(
        page: Int,
        type: MushafType = getCurrentMushafType()
    ): List<VerseWithTranslations> = withContext(Dispatchers.IO) {
        val info = getQuranInfo(type)
        val range: VerseRange = info.getVerseRangeForPage(page)
        val provider = getPageProvider(type)
        val arabicDb = getTextDbHandler(provider.arabicDatabaseName)
        val arabicVerses = arabicDb?.getVerses(range, isArabic = true).orEmpty().associateBy { "${it.sura}:${it.ayah}" }

        val activeFiles = settingsRepository.settings.value.activeTranslationFileNames
        val catalogByFile = translationsRepository.translations.value.associateBy { it.translation.fileName }

        val translationMaps = activeFiles.mapNotNull { fileName ->
            val handler = getTextDbHandler(fileName) ?: return@mapNotNull null
            val displayName = catalogByFile[fileName]?.translation?.displayName
                ?: fileName.removePrefix("quran.").removeSuffix(".db")
            val verses = handler.getVerses(
                verses = range,
                isArabic = false,
                translatorName = displayName
            ).associateBy { "${it.sura}:${it.ayah}" }
            displayName to verses
        }

        val results = mutableListOf<VerseWithTranslations>()
        for (sura in range.startSura..range.endingSura) {
            val firstAyah = if (sura == range.startSura) range.startAyah else 1
            val lastAyah = if (sura == range.endingSura) range.endingAyah else info.getNumberOfAyahs(sura)
            for (ayah in firstAyah..lastAyah) {
                val key = "$sura:$ayah"
                val arabicText = arabicVerses[key]?.text ?: ""
                val transList = translationMaps.mapNotNull { (translatorName, map) ->
                    val quranText = map[key] ?: return@mapNotNull null
                    translatorName to quranText
                }
                results.add(
                    VerseWithTranslations(
                        sura = sura,
                        ayah = ayah,
                        arabicText = arabicText,
                        translations = transList
                    )
                )
            }
        }
        results
    }

    suspend fun getSingleVerseWithTranslations(
        sura: Int,
        ayah: Int,
        type: MushafType = getCurrentMushafType()
    ): VerseWithTranslations = withContext(Dispatchers.IO) {
        val range = VerseRange(sura, ayah, sura, ayah, 1)
        val provider = getPageProvider(type)
        val arabicDb = getTextDbHandler(provider.arabicDatabaseName)
        val arabicText = arabicDb?.getVerses(range, isArabic = true)?.firstOrNull()?.text ?: ""

        val activeFiles = settingsRepository.settings.value.activeTranslationFileNames
        val catalogByFile = translationsRepository.translations.value.associateBy { it.translation.fileName }

        val transList = activeFiles.mapNotNull { fileName ->
            val handler = getTextDbHandler(fileName) ?: return@mapNotNull null
            val displayName = catalogByFile[fileName]?.translation?.displayName
                ?: fileName.removePrefix("quran.").removeSuffix(".db")
            val verse = handler.getVerses(
                verses = range,
                isArabic = false,
                translatorName = displayName
            ).firstOrNull() ?: return@mapNotNull null
            displayName to verse
        }

        VerseWithTranslations(
            sura = sura,
            ayah = ayah,
            arabicText = arabicText,
            translations = transList
        )
    }

    suspend fun searchAcrossDatabases(
        query: String,
        type: MushafType = getCurrentMushafType()
    ): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val info = getQuranInfo(type)
        val provider = getPageProvider(type)
        val results = mutableListOf<SearchResultItem>()

        // 1. Search Arabic Text DB
        val arabicDb = getTextDbHandler(provider.arabicDatabaseName)
        if (arabicDb != null) {
            val sourceLabel = "Arabic (${type.displayName})"
            val hits = arabicDb.search(
                query = trimmed,
                isArabic = true,
                translatorName = sourceLabel
            )
            hits.forEach { hit ->
                results.add(
                    SearchResultItem(
                        sura = hit.sura,
                        ayah = hit.ayah,
                        page = info.getPageFromSuraAyah(hit.sura, hit.ayah),
                        text = hit.text,
                        sourceTitle = sourceLabel,
                        isArabic = true
                    )
                )
            }
        }

        // 2. Search Active Translations
        val activeFiles = settingsRepository.settings.value.activeTranslationFileNames
        val catalogByFile = translationsRepository.translations.value.associateBy { it.translation.fileName }
        for (fileName in activeFiles) {
            val handler = getTextDbHandler(fileName) ?: continue
            val name = catalogByFile[fileName]?.translation?.displayName
                ?: fileName.removePrefix("quran.").removeSuffix(".db")
            val hits = handler.search(
                query = trimmed,
                isArabic = false,
                translatorName = name
            )
            hits.forEach { hit ->
                results.add(
                    SearchResultItem(
                        sura = hit.sura,
                        ayah = hit.ayah,
                        page = info.getPageFromSuraAyah(hit.sura, hit.ayah),
                        text = hit.text,
                        sourceTitle = name,
                        isArabic = false
                    )
                )
            }
        }

        results
    }

    fun formatShareableVerseText(verse: VerseWithTranslations): String {
        val suraName = QuranDisplayData.getSuraName(verse.sura, wantPrefix = true)
        val arabicName = QuranDisplayData.getSuraArabicName(verse.sura)
        return buildString {
            if (verse.arabicText.isNotBlank()) {
                appendLine(verse.arabicText)
                appendLine()
            }
            verse.translations.forEach { (translator, qText) ->
                appendLine(qText.text)
                appendLine("— $translator")
                appendLine()
            }
            append("[$suraName ($arabicName) ${verse.sura}:${verse.ayah}]")
        }
    }
}
