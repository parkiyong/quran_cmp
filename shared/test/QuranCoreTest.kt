package com.jetbrains.kmpapp.data.core

import com.jetbrains.kmpapp.data.model.AyahBounds
import com.jetbrains.kmpapp.data.model.MushafType
import com.jetbrains.kmpapp.data.model.RectF
import com.jetbrains.kmpapp.data.source.MadaniDataSource
import com.jetbrains.kmpapp.data.source.NaskhDataSource
import com.jetbrains.kmpapp.data.source.PageProviderRegistry
import com.jetbrains.kmpapp.data.source.QaloonDataSource
import com.jetbrains.kmpapp.data.source.WarshDataSource
import com.jetbrains.kmpapp.database.AyahInfoDatabaseHandler
import com.jetbrains.kmpapp.database.BookmarksDatabaseHandler
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuranCoreTest {

    @Test
    fun testMadaniQuranInfoMappings() {
        val info = QuranInfo(MadaniDataSource())
        assertEquals(604, info.numberOfPages)
        assertEquals(1, info.getPageNumberForSura(1))
        assertEquals(2, info.getPageNumberForSura(2))
        assertEquals(604, info.getPageNumberForSura(114))
        assertEquals(7, info.getNumberOfAyahs(1))
        assertEquals(286, info.getNumberOfAyahs(2))
        assertEquals(6, info.getNumberOfAyahs(114))
        assertEquals(1, info.getJuzFromPage(1))
        assertEquals(2, info.getJuzFromPage(22))
        assertEquals(30, info.getJuzFromPage(582))
        assertEquals(604, info.getPageFromSuraAyah(114, 6))
    }

    @Test
    fun testAllMushafPageProvidersRegistered() {
        val providers = PageProviderRegistry.allProviders
        assertEquals(5, providers.size)
        assertEquals(604, PageProviderRegistry.getProvider(MushafType.MADANI).dataSource.numberOfPages)
        assertEquals(604, PageProviderRegistry.getProvider(MushafType.TAJWEED).dataSource.numberOfPages)
        assertEquals(604, PageProviderRegistry.getProvider(MushafType.WARSH).dataSource.numberOfPages)
        assertEquals(604, PageProviderRegistry.getProvider(MushafType.QALOON).dataSource.numberOfPages)
        assertEquals(610, PageProviderRegistry.getProvider(MushafType.NASKH).dataSource.numberOfPages)
    }

    @Test
    fun testArabicSearchNormalizer() {
        val withTashkeel = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val normalized = QuranDisplayData.normalizeArabicForSearch(withTashkeel)
        assertTrue(normalized.contains("بسم"))
        assertTrue(normalized.contains("الله"))
    }

    @Test
    fun testBoundingBoxConsolidation() {
        val raw = mutableMapOf(
            "1:1" to mutableListOf(
                AyahBounds(line = 1, position = 1, bounds = RectF(100f, 50f, 200f, 90f)),
                AyahBounds(line = 1, position = 2, bounds = RectF(50f, 52f, 100f, 88f))
            )
        )
        val consolidated = AyahInfoDatabaseHandler.consolidate(raw)
        val list = consolidated["1:1"]
        assertNotNull(list)
        assertEquals(1, list.size)
        assertEquals(50f, list.first().bounds.left)
        assertEquals(200f, list.first().bounds.right)
    }

    @Test
    fun testBookmarksDatabaseHandlerCrud() {
        val tempDb = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "quran_test_bookmarks_${kotlin.random.Random.nextInt()}.db"
        try {
            val handler = try {
                BookmarksDatabaseHandler(tempDb.toString())
            } catch (_: UnsatisfiedLinkError) {
                // sqlite bundled native library not available in host android unit test environment
                return
            }
            assertTrue(handler.togglePageBookmark(42))
            assertTrue(handler.isPageBookmarked(42))

            val ayahBmId = handler.toggleAyahBookmark(sura = 2, ayah = 255, page = 42)
            assertNotNull(ayahBmId)
            assertTrue(handler.isAyahBookmarked(2, 255))

            val tagId = handler.addTag("Memorize")
            assertTrue(tagId > 0)
            handler.updateBookmarkTags(ayahBmId, setOf(tagId))
            val ayahBookmark = handler.bookmarks.value.first { it.id == ayahBmId }
            assertTrue(ayahBookmark.tags.contains(tagId))

            handler.recordRecentPage(42)
            handler.recordRecentPage(100)
            assertEquals(2, handler.recentPages.value.size)

            assertFalse(handler.togglePageBookmark(42))
            assertFalse(handler.isPageBookmarked(42))
        } finally {
            FileSystem.SYSTEM.delete(tempDb, mustExist = false)
        }
    }
}

