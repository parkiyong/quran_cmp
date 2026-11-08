package com.jetbrains.kmpapp.di

import com.jetbrains.kmpapp.data.core.QuranInfo
import com.jetbrains.kmpapp.data.source.MadaniDataSource
import com.jetbrains.kmpapp.database.BookmarksDatabaseHandler
import com.jetbrains.kmpapp.io.QuranDownloadManager
import com.jetbrains.kmpapp.io.QuranFileManager
import com.jetbrains.kmpapp.repository.AudioPlaybackController
import com.jetbrains.kmpapp.repository.QuranRepository
import com.jetbrains.kmpapp.repository.QuranSettingsRepository
import com.jetbrains.kmpapp.repository.TranslationsRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.dsl.module

val quranCoreModule = module {
    single {
        val json = Json { ignoreUnknownKeys = true }
        HttpClient {
            install(ContentNegotiation) {
                json(json, contentType = ContentType.Any)
            }
        }
    }

    single { QuranFileManager() }
    single { QuranDownloadManager(get(), get()) }
    single { QuranSettingsRepository(get()) }
    single {
        val fm: QuranFileManager = get()
        BookmarksDatabaseHandler(fm.getBookmarksDbFilePath())
    }
    single { TranslationsRepository(get(), get(), get(), get()) }
    single { QuranRepository(get(), get(), get(), get(), get()) }
    single {
        AudioPlaybackController(
            fileManager = get(),
            downloadManager = get(),
            settingsRepository = get(),
            quranInfo = QuranInfo(MadaniDataSource())
        )
    }
}

fun initKoin() {
    startKoin {
        modules(quranCoreModule)
    }
}
