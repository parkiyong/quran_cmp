package com.jetbrains.kmpapp

import android.app.Application
import com.jetbrains.kmpapp.di.initKoin
import com.jetbrains.kmpapp.platform.AndroidPlatformContextHolder

class MuseumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidPlatformContextHolder.applicationContext = applicationContext
        initKoin()
    }
}
