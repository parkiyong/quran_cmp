package com.jetbrains.kmpapp.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.jetbrains.kmpapp.data.core.QuranDisplayData
import com.jetbrains.kmpapp.data.model.QuranText
import com.jetbrains.kmpapp.data.model.VerseRange

class QuranTextDatabaseHandler(
    private val dbPath: String
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

    fun getSchemaVersion(): Int = getProperty("schema_version")
    fun getTextVersion(): Int = getProperty("text_version")

    private fun getProperty(prop: String): Int {
        return try {
            withConnection { conn ->
                conn.prepare("SELECT value FROM properties WHERE property = ?").use { stmt ->
                    stmt.bindText(1, prop)
                    if (stmt.step()) stmt.getInt(0) else 1
                }
            }
        } catch (_: Exception) {
            1
        }
    }

    private fun resolveVerseTable(conn: SQLiteConnection, isArabic: Boolean): String {
        val candidates = if (isArabic) {
            listOf("arabic_text", "verses", "share_text")
        } else {
            listOf("verses", "arabic_text")
        }
        for (table in candidates) {
            conn.prepare("SELECT count(1) FROM sqlite_master WHERE type='table' AND name = ?").use { stmt ->
                stmt.bindText(1, table)
                if (stmt.step() && stmt.getInt(0) > 0) {
                    return table
                }
            }
        }
        return if (isArabic) "arabic_text" else "verses"
    }

    private fun resolveSuraCol(conn: SQLiteConnection, table: String): Pair<String, String> {
        conn.prepare("PRAGMA table_info($table)").use { stmt ->
            val cols = mutableSetOf<String>()
            while (stmt.step()) {
                cols.add(stmt.getText(1))
            }
            val suraCol = if ("sura" in cols) "sura" else if ("sura_number" in cols) "sura_number" else "sura"
            val ayahCol = if ("ayah" in cols) "ayah" else if ("ayah_number" in cols) "ayah_number" else "ayah"
            return suraCol to ayahCol
        }
    }

    fun getVerses(
        verses: VerseRange,
        isArabic: Boolean,
        translationId: Int? = null,
        translatorName: String? = null
    ): List<QuranText> {
        return try {
            withConnection { conn ->
                val table = resolveVerseTable(conn, isArabic)
                val (suraCol, ayahCol) = resolveSuraCol(conn, table)
                val whereClause = buildRangeWhereClause(verses, suraCol, ayahCol)
                val sql = "SELECT $suraCol, $ayahCol, text FROM $table WHERE $whereClause ORDER BY $suraCol ASC, $ayahCol ASC"

                val rawList = mutableListOf<QuranText>()
                val toLookup = mutableSetOf<Int>()

                conn.prepare(sql).use { stmt ->
                    while (stmt.step()) {
                        val sura = stmt.getInt(0)
                        val ayah = stmt.getInt(1)
                        val text = if (stmt.isNull(2)) "" else stmt.getText(2)
                        val item = QuranText(
                            sura = sura,
                            ayah = ayah,
                            text = text,
                            translationId = translationId,
                            translatorName = translatorName
                        )
                        rawList.add(item)
                        val linkId = text.trim().toIntOrNull()
                        if (linkId != null && linkId > 0) {
                            toLookup.add(linkId)
                        }
                    }
                }

                if (toLookup.isEmpty()) {
                    rawList
                } else {
                    val expansions = mutableMapOf<Int, String>()
                    val ids = toLookup.joinToString(",")
                    conn.prepare("SELECT rowid, text FROM $table WHERE rowid IN ($ids)").use { stmt ->
                        while (stmt.step()) {
                            expansions[stmt.getInt(0)] = if (stmt.isNull(1)) "" else stmt.getText(1)
                        }
                    }
                    rawList.map { q ->
                        val linkId = q.text.trim().toIntOrNull()
                        if (linkId != null && expansions.containsKey(linkId)) {
                            q.copy(text = expansions[linkId] ?: q.text, extraData = q.text)
                        } else {
                            q
                        }
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getSingleAyahText(sura: Int, ayah: Int, isArabic: Boolean): String? {
        val list = getVerses(VerseRange(sura, ayah, sura, ayah, 1), isArabic)
        return list.firstOrNull()?.text
    }

    fun search(
        query: String,
        isArabic: Boolean,
        translationId: Int? = null,
        translatorName: String? = null,
        limit: Int = 100
    ): List<QuranText> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            withConnection { conn ->
                val table = resolveVerseTable(conn, isArabic)
                val (suraCol, ayahCol) = resolveSuraCol(conn, table)
                val results = mutableListOf<QuranText>()

                if (isArabic) {
                    val normalizedQuery = QuranDisplayData.normalizeArabicForSearch(trimmed)
                    // Check if share_text (without tashkeel) exists for fast LIKE search
                    val hasShareText = run {
                        conn.prepare("SELECT count(1) FROM sqlite_master WHERE type='table' AND name='share_text'").use {
                            it.step() && it.getInt(0) > 0
                        }
                    }
                    val searchTable = if (hasShareText) "share_text" else table
                    val (sCol, aCol) = resolveSuraCol(conn, searchTable)
                    conn.prepare("SELECT $sCol, $aCol, text FROM $searchTable ORDER BY $sCol ASC, $aCol ASC").use { stmt ->
                        while (stmt.step() && results.size < limit) {
                            val sura = stmt.getInt(0)
                            val ayah = stmt.getInt(1)
                            val text = if (stmt.isNull(2)) "" else stmt.getText(2)
                            val normText = QuranDisplayData.normalizeArabicForSearch(text)
                            if (normText.contains(normalizedQuery, ignoreCase = true) ||
                                text.contains(trimmed, ignoreCase = true)
                            ) {
                                results.add(
                                    QuranText(
                                        sura = sura,
                                        ayah = ayah,
                                        text = text,
                                        translationId = translationId,
                                        translatorName = translatorName ?: "Arabic Quran"
                                    )
                                )
                            }
                        }
                    }
                } else {
                    val sql = "SELECT $suraCol, $ayahCol, text FROM $table WHERE text LIKE ? ORDER BY $suraCol ASC, $ayahCol ASC LIMIT ?"
                    conn.prepare(sql).use { stmt ->
                        stmt.bindText(1, "%$trimmed%")
                        stmt.bindInt(2, limit)
                        while (stmt.step()) {
                            val sura = stmt.getInt(0)
                            val ayah = stmt.getInt(1)
                            val text = if (stmt.isNull(2)) "" else stmt.getText(2)
                            if (text.trim().toIntOrNull() == null) {
                                results.add(
                                    QuranText(
                                        sura = sura,
                                        ayah = ayah,
                                        text = text,
                                        translationId = translationId,
                                        translatorName = translatorName
                                    )
                                )
                            }
                        }
                    }
                }
                results
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildRangeWhereClause(verses: VerseRange, suraCol: String, ayahCol: String): String {
        return if (verses.startSura == verses.endingSura) {
            "($suraCol = ${verses.startSura} AND $ayahCol >= ${verses.startAyah} AND $ayahCol <= ${verses.endingAyah})"
        } else {
            "(($suraCol = ${verses.startSura} AND $ayahCol >= ${verses.startAyah}) OR " +
                "($suraCol = ${verses.endingSura} AND $ayahCol <= ${verses.endingAyah}) OR " +
                "($suraCol > ${verses.startSura} AND $suraCol < ${verses.endingSura}))"
        }
    }
}
