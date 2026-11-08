package com.jetbrains.kmpapp.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.jetbrains.kmpapp.data.model.Bookmark
import com.jetbrains.kmpapp.data.model.QuranConstants
import com.jetbrains.kmpapp.data.model.RecentPage
import com.jetbrains.kmpapp.data.model.Tag
import com.jetbrains.kmpapp.platform.PlatformEnvironment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BookmarksDatabaseHandler(
    private val dbPath: String
) {
    private val driver = BundledSQLiteDriver()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _recentPages = MutableStateFlow<List<RecentPage>>(emptyList())
    val recentPages: StateFlow<List<RecentPage>> = _recentPages.asStateFlow()

    init {
        initializeSchema()
        refreshAll()
    }

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

    private fun execSql(conn: SQLiteConnection, sql: String) {
        conn.prepare(sql).use { it.step() }
    }

    private fun initializeSchema() {
        withConnection { conn ->
            execSql(
                conn,
                """
                CREATE TABLE IF NOT EXISTS bookmarks(
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sura INTEGER,
                    ayah INTEGER,
                    page INTEGER NOT NULL,
                    added_date INTEGER DEFAULT (strftime('%s', 'now'))
                )
                """.trimIndent()
            )
            execSql(
                conn,
                """
                CREATE TABLE IF NOT EXISTS tags(
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    added_date INTEGER DEFAULT (strftime('%s', 'now'))
                )
                """.trimIndent()
            )
            execSql(
                conn,
                """
                CREATE TABLE IF NOT EXISTS bookmark_tag(
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    bookmark_id INTEGER NOT NULL,
                    tag_id INTEGER NOT NULL,
                    added_date INTEGER DEFAULT (strftime('%s', 'now')),
                    UNIQUE(bookmark_id, tag_id)
                )
                """.trimIndent()
            )
            execSql(
                conn,
                """
                CREATE TABLE IF NOT EXISTS last_pages(
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    page INTEGER NOT NULL UNIQUE,
                    added_date INTEGER DEFAULT (strftime('%s', 'now'))
                )
                """.trimIndent()
            )
        }
    }

    fun refreshAll() {
        _tags.value = queryTags()
        _bookmarks.value = queryBookmarks()
        _recentPages.value = queryRecentPages()
    }

    private fun queryTags(): List<Tag> {
        return withConnection { conn ->
            val list = mutableListOf<Tag>()
            conn.prepare("SELECT _id, name, added_date FROM tags ORDER BY name ASC").use { stmt ->
                while (stmt.step()) {
                    list.add(
                        Tag(
                            id = stmt.getLong(0),
                            name = stmt.getText(1),
                            timestamp = stmt.getLong(2)
                        )
                    )
                }
            }
            list
        }
    }

    private fun queryBookmarks(): List<Bookmark> {
        return withConnection { conn ->
            val tagMap = mutableMapOf<Long, MutableList<Long>>()
            conn.prepare("SELECT bookmark_id, tag_id FROM bookmark_tag ORDER BY tag_id ASC").use { stmt ->
                while (stmt.step()) {
                    val bId = stmt.getLong(0)
                    val tId = stmt.getLong(1)
                    tagMap.getOrPut(bId) { mutableListOf() }.add(tId)
                }
            }

            val list = mutableListOf<Bookmark>()
            conn.prepare("SELECT _id, sura, ayah, page, added_date FROM bookmarks ORDER BY added_date DESC, _id DESC").use { stmt ->
                while (stmt.step()) {
                    val id = stmt.getLong(0)
                    val sura = if (stmt.isNull(1)) null else stmt.getInt(1)
                    val ayah = if (stmt.isNull(2)) null else stmt.getInt(2)
                    val page = stmt.getInt(3)
                    val ts = stmt.getLong(4)
                    list.add(
                        Bookmark(
                            id = id,
                            sura = sura,
                            ayah = ayah,
                            page = page,
                            timestamp = ts,
                            tags = tagMap[id] ?: emptyList()
                        )
                    )
                }
            }
            list
        }
    }

    private fun queryRecentPages(): List<RecentPage> {
        return withConnection { conn ->
            val list = mutableListOf<RecentPage>()
            conn.prepare("SELECT page, added_date FROM last_pages ORDER BY added_date DESC LIMIT ${QuranConstants.MAX_RECENT_PAGES}").use { stmt ->
                while (stmt.step()) {
                    list.add(
                        RecentPage(
                            page = stmt.getInt(0),
                            timestamp = stmt.getLong(1)
                        )
                    )
                }
            }
            list
        }
    }

    fun isPageBookmarked(page: Int): Boolean {
        return _bookmarks.value.any { it.isPageBookmark && it.page == page }
    }

    fun isAyahBookmarked(sura: Int, ayah: Int): Boolean {
        return _bookmarks.value.any { it.sura == sura && it.ayah == ayah }
    }

    fun togglePageBookmark(page: Int): Boolean {
        val existing = _bookmarks.value.find { it.isPageBookmark && it.page == page }
        return if (existing != null) {
            removeBookmark(existing.id)
            false
        } else {
            addBookmark(sura = null, ayah = null, page = page)
            true
        }
    }

    fun toggleAyahBookmark(sura: Int, ayah: Int, page: Int): Long? {
        val existing = _bookmarks.value.find { it.sura == sura && it.ayah == ayah }
        return if (existing != null) {
            removeBookmark(existing.id)
            null
        } else {
            addBookmark(sura = sura, ayah = ayah, page = page)
        }
    }

    fun addBookmarkIfNotExists(sura: Int?, ayah: Int?, page: Int): Long {
        val existing = _bookmarks.value.find { it.sura == sura && it.ayah == ayah && (sura != null || it.page == page) }
        if (existing != null) return existing.id
        return addBookmark(sura, ayah, page)
    }

    fun addBookmark(sura: Int?, ayah: Int?, page: Int): Long {
        val now = PlatformEnvironment.currentTimeMillis() / 1000L
        val insertedId = withConnection { conn ->
            conn.prepare("INSERT INTO bookmarks(sura, ayah, page, added_date) VALUES (?, ?, ?, ?)").use { stmt ->
                if (sura == null) stmt.bindNull(1) else stmt.bindInt(1, sura)
                if (ayah == null) stmt.bindNull(2) else stmt.bindInt(2, ayah)
                stmt.bindInt(3, page)
                stmt.bindLong(4, now)
                stmt.step()
            }
            conn.prepare("SELECT last_insert_rowid()").use { stmt ->
                if (stmt.step()) stmt.getLong(0) else -1L
            }
        }
        refreshAll()
        return insertedId
    }

    fun removeBookmark(bookmarkId: Long) {
        withConnection { conn ->
            conn.prepare("DELETE FROM bookmark_tag WHERE bookmark_id = ?").use { stmt ->
                stmt.bindLong(1, bookmarkId)
                stmt.step()
            }
            conn.prepare("DELETE FROM bookmarks WHERE _id = ?").use { stmt ->
                stmt.bindLong(1, bookmarkId)
                stmt.step()
            }
        }
        refreshAll()
    }

    fun addTag(name: String): Long {
        val clean = name.trim()
        if (clean.isEmpty()) return -1L
        val existing = _tags.value.find { it.name.equals(clean, ignoreCase = true) }
        if (existing != null) return existing.id

        val now = PlatformEnvironment.currentTimeMillis() / 1000L
        val id = withConnection { conn ->
            conn.prepare("INSERT OR IGNORE INTO tags(name, added_date) VALUES (?, ?)").use { stmt ->
                stmt.bindText(1, clean)
                stmt.bindLong(2, now)
                stmt.step()
            }
            conn.prepare("SELECT _id FROM tags WHERE name = ?").use { stmt ->
                stmt.bindText(1, clean)
                if (stmt.step()) stmt.getLong(0) else -1L
            }
        }
        refreshAll()
        return id
    }

    fun renameTag(tagId: Long, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        withConnection { conn ->
            conn.prepare("UPDATE tags SET name = ? WHERE _id = ?").use { stmt ->
                stmt.bindText(1, clean)
                stmt.bindLong(2, tagId)
                stmt.step()
            }
        }
        refreshAll()
    }

    fun deleteTag(tagId: Long) {
        withConnection { conn ->
            conn.prepare("DELETE FROM bookmark_tag WHERE tag_id = ?").use { stmt ->
                stmt.bindLong(1, tagId)
                stmt.step()
            }
            conn.prepare("DELETE FROM tags WHERE _id = ?").use { stmt ->
                stmt.bindLong(1, tagId)
                stmt.step()
            }
        }
        refreshAll()
    }

    fun updateBookmarkTags(bookmarkId: Long, tagIds: Set<Long>) {
        val now = PlatformEnvironment.currentTimeMillis() / 1000L
        withConnection { conn ->
            conn.prepare("DELETE FROM bookmark_tag WHERE bookmark_id = ?").use { stmt ->
                stmt.bindLong(1, bookmarkId)
                stmt.step()
            }
            for (tagId in tagIds) {
                conn.prepare("INSERT OR IGNORE INTO bookmark_tag(bookmark_id, tag_id, added_date) VALUES (?, ?, ?)").use { stmt ->
                    stmt.bindLong(1, bookmarkId)
                    stmt.bindLong(2, tagId)
                    stmt.bindLong(3, now)
                    stmt.step()
                }
            }
        }
        refreshAll()
    }

    fun recordRecentPage(page: Int) {
        val now = PlatformEnvironment.currentTimeMillis() / 1000L
        withConnection { conn ->
            conn.prepare("INSERT OR REPLACE INTO last_pages(page, added_date) VALUES (?, ?)").use { stmt ->
                stmt.bindInt(1, page)
                stmt.bindLong(2, now)
                stmt.step()
            }
            conn.prepare(
                """
                DELETE FROM last_pages WHERE page NOT IN (
                    SELECT page FROM last_pages ORDER BY added_date DESC LIMIT ${QuranConstants.MAX_RECENT_PAGES}
                )
                """.trimIndent()
            ).use { it.step() }
        }
        refreshAll()
    }

    fun deleteBookmark(bookmarkId: Long) = removeBookmark(bookmarkId)

    fun setBookmarkTags(bookmarkId: Long, tagIds: Set<Long>) =
        updateBookmarkTags(bookmarkId, tagIds)

    fun recordPageNavigation(page: Int) = recordRecentPage(page)

    fun addOrGetAyahBookmark(sura: Int, ayah: Int, page: Int): Long =
        addBookmarkIfNotExists(sura, ayah, page)
}

