package com.jetbrains.kmpapp.io

import com.jetbrains.kmpapp.data.model.Qari
import com.jetbrains.kmpapp.data.source.PageProvider
import com.jetbrains.kmpapp.platform.PlatformEnvironment
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import okio.use

class QuranFileManager(
    val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    val rootDir: Path
        get() {
            val p = PlatformEnvironment.getAppDataDirectory().toPath()
            if (!fileSystem.exists(p)) {
                fileSystem.createDirectories(p)
            }
            return p
        }

    val databasesDir: Path
        get() = ensureDir(rootDir / "databases")

    val audioDatabasesDir: Path
        get() = ensureDir(databasesDir / "audio")

    val audioRootDir: Path
        get() = ensureDir(rootDir / "audio")

    val tempDir: Path
        get() = ensureDir(rootDir / "tmp")

    fun ensureDir(path: Path): Path {
        if (!fileSystem.exists(path)) {
            fileSystem.createDirectories(path)
        }
        return path
    }

    fun getMushafBaseDir(provider: PageProvider): Path =
        ensureDir(rootDir / provider.directoryPrefix)

    fun getMushafImagesDir(provider: PageProvider): Path =
        ensureDir(getMushafBaseDir(provider) / "width${provider.widthParam}")

    fun getMushafDatabaseDir(provider: PageProvider): Path =
        ensureDir(getMushafBaseDir(provider) / "databases")

    fun getAyahInfoDatabasePath(provider: PageProvider): Path =
        getMushafDatabaseDir(provider) / provider.ayahInfoFileName

    fun getArabicDatabasePath(provider: PageProvider): Path =
        databasesDir / provider.arabicDatabaseName

    fun getTranslationDatabasePath(fileName: String): Path =
        databasesDir / fileName

    fun getBookmarksDatabasePath(): Path =
        databasesDir / "quran_bookmarks.db"

    fun getSettingsFilePath(): Path =
        rootDir / "quran_settings.json"

    fun getTranslationsCachePath(): Path =
        databasesDir / "translations.v5.cache.json"

    fun getQariAudioDir(qari: Qari): Path =
        ensureDir(audioRootDir / qari.path)

    fun getGaplessSuraAudioPath(qari: Qari, sura: Int): Path {
        val fileName = sura.toString().padStart(3, '0') + ".mp3"
        return getQariAudioDir(qari) / fileName
    }

    fun getGappedAyahAudioPath(qari: Qari, sura: Int, ayah: Int): Path {
        val fileName = sura.toString().padStart(3, '0') + ayah.toString().padStart(3, '0') + ".mp3"
        return getQariAudioDir(qari) / fileName
    }

    fun getQariTimingDatabasePath(qari: Qari): Path? {
        val dbName = qari.databaseName ?: return null
        return audioDatabasesDir / dbName
    }

    fun getPageFileName(page: Int): String =
        "page" + page.toString().padStart(3, '0') + ".png"

    fun resolvePageImagePath(provider: PageProvider, page: Int): Path {
        val fileName = getPageFileName(page)
        val primary = getMushafImagesDir(provider) / fileName
        if (fileSystem.exists(primary)) return primary

        val nested = getMushafImagesDir(provider) / "width${provider.widthParam}" / fileName
        if (fileSystem.exists(nested)) return nested

        val baseAlt = getMushafBaseDir(provider) / fileName
        if (fileSystem.exists(baseAlt)) return baseAlt

        return primary
    }

    fun hasPageImage(provider: PageProvider, page: Int): Boolean {
        val path = resolvePageImagePath(provider, page)
        return fileSystem.exists(path) && (fileSystem.metadataOrNull(path)?.size ?: 0L) > 0L
    }

    fun hasAllPageImages(provider: PageProvider): Boolean {
        val firstPage = provider.dataSource.pagesToSkip + 1
        val lastPage = provider.dataSource.numberOfPages
        val midPage = (firstPage + lastPage) / 2
        return hasPageImage(provider, firstPage) &&
            hasPageImage(provider, midPage) &&
            hasPageImage(provider, lastPage)
    }

    fun hasAyahInfoDatabase(provider: PageProvider): Boolean {
        val path = getAyahInfoDatabasePath(provider)
        return fileSystem.exists(path) && (fileSystem.metadataOrNull(path)?.size ?: 0L) > 1024L
    }

    fun hasArabicDatabase(provider: PageProvider): Boolean {
        val path = getArabicDatabasePath(provider)
        return fileSystem.exists(path) && (fileSystem.metadataOrNull(path)?.size ?: 0L) > 1024L
    }

    fun isMushafReady(provider: PageProvider): Boolean {
        return hasAllPageImages(provider) && hasAyahInfoDatabase(provider)
    }

    fun extractZip(zipFile: Path, destinationDir: Path) {
        ensureDir(destinationDir)
        val zipFs = fileSystem.openZip(zipFile)
        fun copyRecursively(dir: Path) {
            for (child in zipFs.list(dir)) {
                val meta = zipFs.metadataOrNull(child) ?: continue
                if (meta.isDirectory) {
                    copyRecursively(child)
                } else {
                    val relName = child.name
                    if (relName.startsWith(".")) continue
                    val outPath = destinationDir / relName
                    outPath.parent?.let { ensureDir(it) }
                    zipFs.source(child).buffer().use { src ->
                        fileSystem.sink(outPath).buffer().use { dst ->
                            dst.writeAll(src)
                        }
                    }
                }
            }
        }
        copyRecursively("/".toPath())
    }

    fun deletePathRecursively(path: Path) {
        if (fileSystem.exists(path)) {
            fileSystem.deleteRecursively(path)
        }
    }

    fun getAppRootDirectory(): String = rootDir.toString()

    fun getDatabaseFilePath(fileName: String): String =
        getTranslationDatabasePath(fileName).toString()

    fun getBookmarksDbFilePath(): String =
        getBookmarksDatabasePath().toString()

    fun getAyahInfoDbFilePath(provider: PageProvider): String =
        getAyahInfoDatabasePath(provider).toString()

    fun getPageImagePath(provider: PageProvider, page: Int): String =
        resolvePageImagePath(provider, page).toString()

    fun getGaplessTimingDbPath(dbName: String): String {
        val clean = if (dbName.endsWith(".db")) dbName else "$dbName.db"
        return (audioDatabasesDir / clean).toString()
    }

    fun getGaplessSuraAudioPath(qariPath: String, sura: Int): String {
        val fileName = sura.toString().padStart(3, '0') + ".mp3"
        return (ensureDir(audioRootDir / qariPath) / fileName).toString()
    }

    fun getGappedAyahAudioPath(qariPath: String, sura: Int, ayah: Int): String {
        val fileName = sura.toString().padStart(3, '0') + ayah.toString().padStart(3, '0') + ".mp3"
        return (ensureDir(audioRootDir / qariPath) / fileName).toString()
    }

    fun fileExists(pathStr: String): Boolean {
        val p = pathStr.toPath()
        return fileSystem.exists(p) && (fileSystem.metadataOrNull(p)?.size ?: 0L) > 0L
    }

    fun deletePath(pathStr: String) {
        deletePathRecursively(pathStr.toPath())
    }

    fun isMushafBundleComplete(
        provider: PageProvider,
        totalPages: Int = provider.dataSource.numberOfPages
    ): Boolean = isMushafReady(provider)
}

