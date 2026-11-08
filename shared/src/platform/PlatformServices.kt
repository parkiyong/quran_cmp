package com.jetbrains.kmpapp.platform

import androidx.compose.ui.graphics.ImageBitmap
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

expect object PlatformEnvironment {
    fun getAppDataDirectory(): String
    fun currentTimeMillis(): Long
}

expect class PlatformAudioPlayer() {
    fun play(
        fileOrUrl: String,
        startPositionMs: Long = 0L,
        speed: Float = 1.0f,
        onCompletion: () -> Unit = {},
        onError: (String) -> Unit = {}
    )
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun getCurrentPositionMs(): Long
    fun isPlaying(): Boolean
    fun release()
}

expect object PlatformShareService {
    fun copyToClipboard(text: String)
    fun shareText(text: String, title: String = "Share Ayah")
    fun sharePngImage(pngBytes: ByteArray, fileName: String, textCaption: String): String
    fun encodeImageBitmapToPng(imageBitmap: ImageBitmap): ByteArray
    fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?
}

fun decodeImageBitmapFromFile(filePath: String): ImageBitmap? {
    return try {
        val path = filePath.toPath()
        if (!FileSystem.SYSTEM.exists(path)) return null
        val bytes = FileSystem.SYSTEM.read(path) { readByteArray() }
        if (bytes.isEmpty()) null else PlatformShareService.decodeImageBitmap(bytes)
    } catch (_: Exception) {
        null
    }
}

fun encodeImageBitmapToPngBytes(bitmap: ImageBitmap): ByteArray? =
    PlatformShareService.encodeImageBitmapToPng(bitmap).takeIf { it.isNotEmpty() }

fun copyTextToClipboard(text: String) =
    PlatformShareService.copyToClipboard(text)

fun shareText(text: String, title: String = "Share Ayah") =
    PlatformShareService.shareText(text, title)

fun shareOrSavePngImage(pngBytes: ByteArray, fileName: String, textCaption: String): String? =
    PlatformShareService.sharePngImage(pngBytes, fileName, textCaption)
