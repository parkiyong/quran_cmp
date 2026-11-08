package com.jetbrains.kmpapp.data.core

import com.jetbrains.kmpapp.data.model.QuranConstants.LAST_SURA
import com.jetbrains.kmpapp.data.model.QuranConstants.MAX_AYAH
import com.jetbrains.kmpapp.data.model.QuranConstants.MIN_AYAH
import com.jetbrains.kmpapp.data.model.QuranConstants.NUMBER_OF_SURAS
import com.jetbrains.kmpapp.data.model.QuranConstants.PAGES_FIRST
import com.jetbrains.kmpapp.data.model.SuraAyah
import com.jetbrains.kmpapp.data.model.VerseRange
import com.jetbrains.kmpapp.data.source.QuranDataSource
import kotlin.math.abs

class QuranInfo(private val quranDataSource: QuranDataSource) {
    val suraPageStart: IntArray = quranDataSource.pageForSuraArray
    private val pageSuraStart: IntArray = quranDataSource.suraForPageArray
    private val pageAyahStart: IntArray = quranDataSource.ayahForPageArray
    private val juzPageStart: IntArray = quranDataSource.pageForJuzArray
    private val juzPageOverride: Map<Int, Int> = quranDataSource.juzDisplayPageArrayOverride
    private val pageRub3Start: IntArray = quranDataSource.quarterStartByPage
    private val suraNumAyahs: IntArray = quranDataSource.numberOfAyahsForSuraArray
    private val suraIsMakki: BooleanArray = quranDataSource.isMakkiBySuraArray
    private val manazil: Array<Int> = quranDataSource.manzilPageArray
    val quarters: Array<SuraAyah> = quranDataSource.quartersArray

    val skip: Int = quranDataSource.pagesToSkip
    val firstPage: Int = PAGES_FIRST + skip

    val numberOfPages: Int = quranDataSource.numberOfPages
    val numberOfPagesConsideringSkipped: Int = numberOfPages - skip
    private val numberOfPagesDual: Int =
        numberOfPagesConsideringSkipped / 2 + numberOfPagesConsideringSkipped % 2

    fun getStartingPageForJuz(juz: Int): Int {
        if (juz < 1 || juz > juzPageStart.size) return 1
        return juzPageStart[juz - 1]
    }

    fun getPageNumberForSura(sura: Int): Int {
        if (sura < 1 || sura > NUMBER_OF_SURAS) return 1
        return suraPageStart[sura - 1]
    }

    fun manzilForPage(page: Int): Int {
        val manzil = manazil.indexOfFirst { it > page }
        return if (manzil == -1 && manazil.isNotEmpty() && page >= manazil.last()) {
            manazil.size
        } else {
            manzil
        }
    }

    fun isValidPage(page: Int): Boolean = page in firstPage..numberOfPages

    fun isMakki(sura: Int): Boolean {
        if (sura < 1 || sura > NUMBER_OF_SURAS) return true
        return suraIsMakki[sura - 1]
    }

    fun getSuraNumberFromPage(page: Int): Int {
        var sura = -1
        for (i in 0 until NUMBER_OF_SURAS) {
            if (suraPageStart[i] == page) {
                sura = i + 1
                break
            } else if (suraPageStart[i] > page) {
                sura = i
                break
            }
        }
        if (sura == -1 && isValidPage(page)) {
            sura = pageSuraStart[(page - 1).coerceIn(pageSuraStart.indices)]
        }
        return sura
    }

    fun getListOfSurahWithStartingOnPage(page: Int): List<Int> {
        if (!isValidPage(page)) return emptyList()
        val startIndex = (pageSuraStart[page - 1] - 1).coerceAtLeast(0)
        val result = mutableListOf<Int>()
        for (i in startIndex until NUMBER_OF_SURAS) {
            if (suraPageStart[i] == page) {
                result.add(i + 1)
            } else if (suraPageStart[i] > page) break
        }
        return result
    }

    fun getVerseRangeForPage(page: Int): VerseRange {
        val result = getPageBounds(page)
        val versesInRange: Int = 1 + abs(
            getAyahId(result[0], result[1]) - getAyahId(result[2], result[3])
        )
        return VerseRange(result[0], result[1], result[2], result[3], versesInRange)
    }

    fun getFirstAyahOnPage(page: Int): Int {
        if (!isValidPage(page)) return 1
        return pageAyahStart[page - 1]
    }

    fun getPageBounds(inputPage: Int): IntArray {
        val page = when {
            inputPage > numberOfPages -> numberOfPages
            inputPage < firstPage -> firstPage
            else -> inputPage
        }

        val bounds = IntArray(4)
        bounds[0] = pageSuraStart[page - 1]
        bounds[1] = pageAyahStart[page - 1]
        if (page == numberOfPages) {
            bounds[2] = LAST_SURA
            bounds[3] = 6
        } else {
            val nextPageSura = pageSuraStart[page]
            val nextPageAyah = pageAyahStart[page]
            if (nextPageSura == bounds[0]) {
                bounds[2] = bounds[0]
                bounds[3] = nextPageAyah - 1
            } else {
                if (nextPageAyah > 1) {
                    bounds[2] = nextPageSura
                    bounds[3] = nextPageAyah - 1
                } else {
                    bounds[2] = nextPageSura - 1
                    bounds[3] = suraNumAyahs[bounds[2] - 1]
                }
            }
        }
        return bounds
    }

    fun getSuraOnPage(page: Int): Int =
        pageSuraStart[(page - 1).coerceIn(pageSuraStart.indices)]

    fun getJuzFromPage(page: Int): Int {
        for (i in juzPageStart.indices) {
            if (juzPageStart[i] > page) {
                return i
            } else if (juzPageStart[i] == page) {
                return i + 1
            }
        }
        return 30
    }

    fun getJuzForDisplayFromPage(page: Int): Int {
        val actualJuz = getJuzFromPage(page)
        return juzPageOverride[page] ?: actualJuz
    }

    fun getRub3FromPage(page: Int): Int {
        return if (page > numberOfPages || page < 1) -1 else pageRub3Start[page - 1]
    }

    fun getPageFromSuraAyah(sura: Int, ayah: Int): Int {
        val currentAyah = if (ayah == 0) 1 else ayah
        if (sura < 1 || sura > NUMBER_OF_SURAS || currentAyah < MIN_AYAH || currentAyah > MAX_AYAH) {
            return -1
        }

        var index = suraPageStart[sura - 1] - 1
        while (index < numberOfPages) {
            val ss = pageSuraStart[index]
            if (ss > sura || (ss == sura && pageAyahStart[index] > currentAyah)) {
                break
            }
            index++
        }
        return index
    }

    fun getAyahId(sura: Int, ayah: Int): Int {
        var ayahId = 0
        for (i in 0 until (sura - 1).coerceIn(0, NUMBER_OF_SURAS)) {
            ayahId += suraNumAyahs[i]
        }
        ayahId += ayah
        return ayahId
    }

    fun getSuraAyahFromAyahId(ayahId: Int): SuraAyah {
        var sura = 1
        var ayahIdentifier = ayahId
        while (sura <= NUMBER_OF_SURAS && ayahIdentifier > suraNumAyahs[sura - 1]) {
            ayahIdentifier -= suraNumAyahs[sura - 1]
            sura++
        }
        return SuraAyah(sura.coerceIn(1, NUMBER_OF_SURAS), ayahIdentifier.coerceAtLeast(1))
    }

    fun diff(start: SuraAyah, end: SuraAyah): Int {
        return getAyahId(end.sura, end.ayah) - getAyahId(start.sura, start.ayah)
    }

    fun getNumberOfAyahs(sura: Int): Int {
        return if (sura < 1 || sura > NUMBER_OF_SURAS) -1 else suraNumAyahs[sura - 1]
    }

    fun getNumberOfAyahsInQuran(): Int = suraNumAyahs.sum()

    fun getNextAyah(current: SuraAyah): SuraAyah? {
        val maxAyah = getNumberOfAyahs(current.sura)
        return when {
            current.ayah < maxAyah -> SuraAyah(current.sura, current.ayah + 1)
            current.sura < LAST_SURA -> SuraAyah(current.sura + 1, 1)
            else -> null
        }
    }

    fun getPreviousAyah(current: SuraAyah): SuraAyah? {
        return when {
            current.ayah > 1 -> SuraAyah(current.sura, current.ayah - 1)
            current.sura > 1 -> {
                val prevSura = current.sura - 1
                SuraAyah(prevSura, getNumberOfAyahs(prevSura))
            }
            else -> null
        }
    }

    fun getAyahsOnPage(page: Int): List<SuraAyah> {
        if (!isValidPage(page)) return emptyList()
        val bounds = getPageBounds(page)
        val start = SuraAyah(bounds[0], bounds[1])
        val end = SuraAyah(bounds[2], bounds[3])
        val list = mutableListOf<SuraAyah>()
        var curr: SuraAyah? = start
        while (curr != null && curr <= end) {
            list.add(curr)
            curr = getNextAyah(curr)
        }
        return list
    }

    fun getJuzFromSuraAyah(sura: Int, ayah: Int, defaultJuz: Int): Int {
        if (defaultJuz == 30) return defaultJuz
        val lastQuarter = quarters[(defaultJuz * 8).coerceAtMost(quarters.size - 1)]
        val verse = SuraAyah(sura, ayah)
        return if (verse >= lastQuarter) defaultJuz + 1 else defaultJuz
    }
}
