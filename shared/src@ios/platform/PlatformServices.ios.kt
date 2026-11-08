package com.jetbrains.kmpapp.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIPasteboard

@OptIn(ExperimentalForeignApi::class)
actual object PlatformEnvironment {
    actual fun getAppDataDirectory(): String {
        val urls = NSFileManager.defaultManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask
        )
        val docUrl = urls.firstOrNull() as? NSURL
        val basePath = docUrl?.path ?: "/tmp"
        val quranPath = "$basePath/quran_android"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = quranPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        return quranPath
    }

    actual fun currentTimeMillis(): Long =
        (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}

@OptIn(ExperimentalForeignApi::class)
actual class PlatformAudioPlayer actual constructor() {
    private var player: AVPlayer? = null
    private var playingState = false
    private var currentSpeed = 1.0f

    actual fun play(
        fileOrUrl: String,
        startPositionMs: Long,
        speed: Float,
        onCompletion: () -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        currentSpeed = speed
        try {
            val url = if (fileOrUrl.startsWith("http://") || fileOrUrl.startsWith("https://")) {
                NSURL.URLWithString(fileOrUrl)
            } else {
                NSURL.fileURLWithPath(fileOrUrl)
            }
            if (url == null) {
                onError("Invalid audio URL: $fileOrUrl")
                return
            }
            val item = AVPlayerItem(uRL = url)
            val avPlayer = AVPlayer(playerItem = item)
            player = avPlayer
            if (startPositionMs > 0) {
                avPlayer.seekToTime(CMTimeMakeWithSeconds(startPositionMs / 1000.0, 1000))
            }
            avPlayer.play()
            avPlayer.rate = speed
            playingState = true
        } catch (e: Exception) {
            onError(e.message ?: "iOS audio playback error")
        }
    }

    actual fun pause() {
        player?.pause()
        playingState = false
    }

    actual fun resume() {
        player?.play()
        player?.rate = currentSpeed
        playingState = true
    }

    actual fun stop() {
        player?.pause()
        player = null
        playingState = false
    }

    actual fun seekTo(positionMs: Long) {
        player?.seekToTime(CMTimeMakeWithSeconds(positionMs / 1000.0, 1000))
    }

    actual fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        if (playingState) {
            player?.rate = speed
        }
    }

    actual fun getCurrentPositionMs(): Long {
        val p = player ?: return 0L
        val seconds = CMTimeGetSeconds(p.currentTime())
        return if (seconds.isNaN() || seconds < 0.0) 0L else (seconds * 1000.0).toLong()
    }

    actual fun isPlaying(): Boolean = playingState && (player?.rate ?: 0f) > 0f

    actual fun release() {
        stop()
    }
}

actual object PlatformShareService {
    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    actual fun shareText(text: String, title: String) {
        copyToClipboard(text)
    }

    actual fun sharePngImage(pngBytes: ByteArray, fileName: String, textCaption: String): String {
        copyToClipboard(textCaption)
        return "${PlatformEnvironment.getAppDataDirectory()}/$fileName"
    }

    actual fun encodeImageBitmapToPng(imageBitmap: ImageBitmap): ByteArray {
        val skiaImage = Image.makeFromBitmap(imageBitmap.asSkiaBitmap())
        val data = skiaImage.encodeToData(EncodedImageFormat.PNG, 100)
        return data?.bytes ?: ByteArray(0)
    }

    actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
        return try {
            val skiaImage = Image.makeFromEncoded(bytes)
            skiaImage.toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

