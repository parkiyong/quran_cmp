package com.jetbrains.kmpapp

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.jetbrains.kmpapp.di.initKoin

fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Quran Multiplatform",
            state = WindowState(size = DpSize(1024.dp, 820.dp))
        ) {
            App()
        }
    }
}
