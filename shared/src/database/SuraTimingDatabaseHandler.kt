package com.jetbrains.kmpapp.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

data class AyahTimingEntry(
    val sura: Int,
    val ayah: Int,
    val timeMs: Long
)

class SuraTimingDatabaseHandler(
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

    fun getAyahTimingsForSura(sura: Int): List<AyahTimingEntry> {
        return try {
            withConnection { conn ->
                val list = mutableListOf<AyahTimingEntry>()
                conn.prepare("SELECT sura, ayah, time FROM timings WHERE sura = ? ORDER BY ayah ASC").use { stmt ->
                    stmt.bindInt(1, sura)
                    while (stmt.step()) {
                        list.add(
                            AyahTimingEntry(
                                sura = stmt.getInt(0),
                                ayah = stmt.getInt(1),
                                timeMs = stmt.getLong(2)
                            )
                        )
                    }
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun findAyahAtTimeMs(timings: List<AyahTimingEntry>, positionMs: Long): Int? {
        if (timings.isEmpty()) return null
        var currentAyah = timings.first().ayah
        for (entry in timings) {
            if (entry.ayah == 999) break // End of sura marker in quran_android timing dbs
            if (positionMs >= entry.timeMs) {
                currentAyah = entry.ayah
            } else {
                break
            }
        }
        return currentAyah.takeIf { it in 1..286 }
    }

    fun findStartTimeMsForAyah(timings: List<AyahTimingEntry>, ayah: Int): Long {
        return timings.find { it.ayah == ayah }?.timeMs ?: 0L
    }
}
