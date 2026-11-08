package com.jetbrains.kmpapp.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream
import java.io.File

object AndroidPlatformContextHolder {
    var applicationContext: Context? = null
}

actual object PlatformEnvironment {
    actual fun getAppDataDirectory(): String {
        val ctx = AndroidPlatformContextHolder.applicationContext
        val dir = ctx?.filesDir ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp", "quran_cmp")
        val quranDir = File(dir, "quran_android")
        if (!quranDir.exists()) {
            quranDir.mkdirs()
        }
        return quranDir.absolutePath
    }

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}

actual class PlatformAudioPlayer actual constructor() {
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
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
            val mp = MediaPlayer()
            mediaPlayer = mp
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mp.setDataSource(fileOrUrl)
            mp.setOnPreparedListener { player ->
                isPrepared = true
                if (startPositionMs > 0) {
                    player.seekTo(startPositionMs.toInt())
                }
                applySpeed(player, currentSpeed)
                player.start()
            }
            mp.setOnCompletionListener {
                onCompletion()
            }
            mp.setOnErrorListener { _, what, extra ->
                onError("MediaPlayer error ($what, $extra)")
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            onError(e.message ?: "Failed to start audio playback")
        }
    }

    private fun applySpeed(player: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                player.playbackParams = PlaybackParams().setSpeed(speed.coerceIn(0.5f, 2.0f))
            } catch (_: Exception) {
            }
        }
    }

    actual fun pause() {
        try {
            if (isPrepared && mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {
        }
    }

    actual fun resume() {
        try {
            if (isPrepared && mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (_: Exception) {
        }
    }

    actual fun stop() {
        isPrepared = false
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    actual fun seekTo(positionMs: Long) {
        try {
            if (isPrepared) {
                mediaPlayer?.seekTo(positionMs.toInt())
            }
        } catch (_: Exception) {
        }
    }

    actual fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        val mp = mediaPlayer ?: return
        if (isPrepared) {
            applySpeed(mp, speed)
        }
    }

    actual fun getCurrentPositionMs(): Long {
        return try {
            if (isPrepared) mediaPlayer?.currentPosition?.toLong() ?: 0L else 0L
        } catch (_: Exception) {
            0L
        }
    }

    actual fun isPlaying(): Boolean {
        return try {
            isPrepared && mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    actual fun release() {
        stop()
    }
}

actual object PlatformShareService {
    actual fun copyToClipboard(text: String) {
        val ctx = AndroidPlatformContextHolder.applicationContext ?: return
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Quran Ayah", text))
    }

    actual fun shareText(text: String, title: String) {
        val ctx = AndroidPlatformContextHolder.applicationContext ?: return
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(chooser)
    }

    actual fun sharePngImage(pngBytes: ByteArray, fileName: String, textCaption: String): String {
        val dir = File(PlatformEnvironment.getAppDataDirectory(), "shared_cards")
        dir.mkdirs()
        val outFile = File(dir, fileName)
        outFile.writeBytes(pngBytes)
        shareText("$textCaption\n\n[Saved Ayah Card: ${outFile.absolutePath}]", "Share Ayah Card")
        return outFile.absolutePath
    }

    actual fun encodeImageBitmapToPng(imageBitmap: ImageBitmap): ByteArray {
        val bitmap = imageBitmap.asAndroidBitmap()
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
        return try {
            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            androidx.compose.ui.graphics.asImageBitmap(bmp)
        } catch (_: Exception) {
            null
        }
    }
}

