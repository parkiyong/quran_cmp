package com.jetbrains.kmpapp.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.jetbrains.kmpapp.data.model.AyahBounds
import com.jetbrains.kmpapp.data.model.AyahCoordinates
import com.jetbrains.kmpapp.data.model.AyahMarkerLocation
import com.jetbrains.kmpapp.data.model.PageCoordinates
import com.jetbrains.kmpapp.data.model.RectF
import com.jetbrains.kmpapp.data.model.SuraHeaderLocation
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AyahInfoDatabaseHandler(
    private val dbPath: String,
    private val pageWidth: Int = 1024
) {
    private val driver = BundledSQLiteDriver()

    private inline fun <T> withConnection(block: (SQLiteConnection) -> T): T {
        val conn = driver.open(dbPath)
        return try {
            block(conn)
        } finally {
            try {
                conn.close()
            } catch (_: Exception) {
            }
        }
    }

    fun getPageCoordinates(page: Int, wantBounds: Boolean = true): PageCoordinates {
        return withConnection { conn ->
            val bounds = if (wantBounds) queryPageBounds(conn, page) else RectF()
            val hasMarkers = tableExists(conn, "ayah_markers")
            val hasHeaders = tableExists(conn, "sura_headers")
            val headers = if (hasHeaders) querySuraHeaders(conn, page) else emptyList()
            val markers = if (hasMarkers) queryAyahMarkers(conn, page) else emptyList()
            PageCoordinates(
                page = page,
                pageBounds = bounds,
                suraHeaders = headers,
                ayahMarkers = markers
            )
        }
    }

    fun getAyahCoordinatesForPage(page: Int, underlineRaiseRatio: Float = 0f): AyahCoordinates {
        return withConnection { conn ->
            val rawMap = LinkedHashMap<String, MutableList<AyahBounds>>()
            val sql = """
                SELECT line_number, sura_number, ayah_number, position, min_x, min_y, max_x, max_y
                FROM glyphs
                WHERE page_number = ?
                ORDER BY sura_number ASC, ayah_number ASC, position ASC
            """.trimIndent()
            conn.prepare(sql).use { stmt ->
                stmt.bindInt(1, page)
                while (stmt.step()) {
                    val line = stmt.getInt(0)
                    val sura = stmt.getInt(1)
                    val ayah = stmt.getInt(2)
                    val position = stmt.getInt(3)
                    val minX = stmt.getInt(4)
                    val minY = stmt.getInt(5)
                    val maxX = stmt.getInt(6)
                    val maxY = stmt.getInt(7)

                    val key = "$sura:$ayah"
                    val list = rawMap.getOrPut(key) { mutableListOf() }
                    val bound = AyahBounds(line, position, minX, minY, maxX, maxY)
                    val last = list.lastOrNull()
                    if (last != null && last.line == bound.line) {
                        last.engulf(bound)
                    } else {
                        list.add(bound)
                    }
                }
            }
            normalizePageAyahs(page, rawMap, underlineRaiseRatio)
        }
    }

    fun getAyahCoordinates(page: Int): AyahCoordinates =
        getAyahCoordinatesForPage(page)

    private fun queryPageBounds(conn: SQLiteConnection, page: Int): RectF {
        val sql = "SELECT MIN(min_x), MIN(min_y), MAX(max_x), MAX(max_y) FROM glyphs WHERE page_number = ?"
        conn.prepare(sql).use { stmt ->
            stmt.bindInt(1, page)
            if (stmt.step()) {
                return RectF(
                    left = stmt.getInt(0).toFloat(),
                    top = stmt.getInt(1).toFloat(),
                    right = stmt.getInt(2).toFloat(),
                    bottom = stmt.getInt(3).toFloat()
                )
            }
        }
        return RectF(0f, 0f, pageWidth.toFloat(), (pageWidth * 1.6f))
    }

    private fun tableExists(conn: SQLiteConnection, tableName: String): Boolean {
        conn.prepare("SELECT count(1) FROM sqlite_master WHERE name = ?").use { stmt ->
            stmt.bindText(1, tableName)
            return stmt.step() && stmt.getInt(0) > 0
        }
    }

    private fun querySuraHeaders(conn: SQLiteConnection, page: Int): List<SuraHeaderLocation> {
        val result = mutableListOf<SuraHeaderLocation>()
        val sql = "SELECT sura_number, x, y, width, height FROM sura_headers WHERE page_number = ? ORDER BY sura_number ASC"
        conn.prepare(sql).use { stmt ->
            stmt.bindInt(1, page)
            while (stmt.step()) {
                result.add(
                    SuraHeaderLocation(
                        sura = stmt.getInt(0),
                        x = stmt.getInt(1),
                        y = stmt.getInt(2),
                        width = stmt.getInt(3),
                        height = stmt.getInt(4)
                    )
                )
            }
        }
        return result
    }

    private fun queryAyahMarkers(conn: SQLiteConnection, page: Int): List<AyahMarkerLocation> {
        val result = mutableListOf<AyahMarkerLocation>()
        val sql = "SELECT sura_number, ayah_number, x, y FROM ayah_markers WHERE page_number = ? ORDER BY sura_number, ayah_number ASC"
        conn.prepare(sql).use { stmt ->
            stmt.bindInt(1, page)
            while (stmt.step()) {
                result.add(
                    AyahMarkerLocation(
                        sura = stmt.getInt(0),
                        ayah = stmt.getInt(1),
                        x = stmt.getInt(2),
                        y = stmt.getInt(3)
                    )
                )
            }
        }
        return result
    }

    private fun normalizePageAyahs(
        page: Int,
        original: Map<String, List<AyahBounds>>,
        underlineRaiseRatio: Float
    ): AyahCoordinates {
        val ayahLineCoordinates = alignWithLines(original)
        val normalizedMap = LinkedHashMap<String, List<AyahBounds>>()
        for ((key, bounds) in original) {
            val copyBounds = bounds.map { it.copy(bounds = it.bounds.copy()) }
            normalizedMap[key] = normalizeAyahBounds(copyBounds)
        }
        return AyahCoordinates(
            page = page,
            ayahCoordinates = normalizedMap,
            ayahLineCoordinates = ayahLineCoordinates,
            underlineRaiseRatio = underlineRaiseRatio
        )
    }

    private fun alignWithLines(ayahCoordinates: Map<String, List<AyahBounds>>): Map<String, List<AyahBounds>> {
        val lineBounds = ayahCoordinates.values
            .flatten()
            .groupBy { it.line }
            .mapValues { (_, bounds) ->
                bounds.fold(RectF()) { line, bound -> line.union(bound.bounds) }
            }

        return ayahCoordinates.mapValues { (_, bounds) ->
            bounds.map { bound ->
                val line = lineBounds[bound.line] ?: bound.bounds
                val ayahBounds = bound.bounds
                bound.withBounds(RectF(ayahBounds.left, line.top, ayahBounds.right, line.bottom))
            }
        }
    }

    private fun normalizeAyahBounds(ayahBounds: List<AyahBounds>): List<AyahBounds> {
        val total = ayahBounds.size
        return when {
            total < 2 -> ayahBounds
            total < 3 -> consolidate(ayahBounds[0], ayahBounds[1])
            else -> {
                val middle = ayahBounds[1].copy(bounds = ayahBounds[1].bounds.copy())
                for (i in 2 until total - 1) {
                    middle.engulf(ayahBounds[i])
                }
                var top = consolidate(ayahBounds[0], middle)
                val topSize = top.size
                val bottom = consolidate(top[topSize - 1], ayahBounds[total - 1])
                if (topSize == 1) {
                    bottom
                } else if (topSize + bottom.size > 4) {
                    val result = mutableListOf<AyahBounds>()
                    for (i in 0 until topSize - 1) {
                        result.add(top[i])
                    }
                    val lastTop = top[topSize - 1]
                    val firstBottom = bottom[0]
                    if (lastTop == firstBottom) {
                        result.add(lastTop)
                    } else if (lastTop.bounds.contains(firstBottom.bounds)) {
                        result.add(lastTop)
                    } else if (firstBottom.bounds.contains(lastTop.bounds)) {
                        result.add(firstBottom)
                    } else {
                        result.add(lastTop)
                        result.add(firstBottom)
                    }
                    for (i in 1 until bottom.size) {
                        result.add(bottom[i])
                    }
                    result
                } else {
                    val result = mutableListOf<AyahBounds>()
                    top = consolidate(top[0], bottom[0])
                    result.addAll(top)
                    if (bottom.size > 1) {
                        result.add(bottom[1])
                    }
                    result
                }
            }
        }
    }

    private fun consolidate(topIn: AyahBounds, bottomIn: AyahBounds): List<AyahBounds> {
        var top = topIn.copy(bounds = topIn.bounds.copy())
        var bottom = bottomIn.copy(bounds = bottomIn.bounds.copy())
        val firstRect = top.bounds
        val lastRect = bottom.bounds
        var middle: AyahBounds? = null

        val threshold = (0.015f * pageWidth).toInt()
        val firstIsFullLine = abs(firstRect.right - lastRect.right) < threshold
        val secondIsFullLine = abs(firstRect.left - lastRect.left) < threshold

        if (firstIsFullLine && secondIsFullLine) {
            top.engulf(bottom)
            return listOf(top)
        } else if (firstIsFullLine) {
            lastRect.top = firstRect.bottom
            val bestStartOfLine = max(firstRect.right, lastRect.right)
            firstRect.right = bestStartOfLine
            lastRect.right = bestStartOfLine
            top = top.withBounds(firstRect)
            bottom = bottom.withBounds(lastRect)
        } else if (secondIsFullLine) {
            firstRect.bottom = lastRect.top
            val bestEndOfLine = min(firstRect.left, lastRect.left)
            firstRect.left = bestEndOfLine
            lastRect.left = bestEndOfLine
            top = top.withBounds(firstRect)
            bottom = bottom.withBounds(lastRect)
        } else if (lastRect.left < firstRect.right) {
            val middleBounds = RectF(
                left = lastRect.left,
                top = firstRect.bottom,
                right = min(firstRect.right, lastRect.right),
                bottom = lastRect.top
            )
            middle = AyahBounds(top.line, top.position, middleBounds)
        }

        val result = mutableListOf<AyahBounds>()
        result.add(top)
        if (middle != null) result.add(middle)
        result.add(bottom)
        return result
    }

    companion object {
        fun consolidate(raw: Map<String, List<AyahBounds>>): Map<String, List<AyahBounds>> {
            return raw.mapValues { (_, list) ->
                val merged = mutableListOf<AyahBounds>()
                for (b in list) {
                    val last = merged.lastOrNull()
                    if (last != null && last.line == b.line) {
                        val copy = last.copy(bounds = last.bounds.copy())
                        copy.engulf(b)
                        merged[merged.lastIndex] = copy
                    } else {
                        merged.add(b.copy(bounds = b.bounds.copy()))
                    }
                }
                merged
            }
        }
    }
}

