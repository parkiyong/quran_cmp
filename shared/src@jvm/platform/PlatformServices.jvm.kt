package com.jetbrains.kmpapp.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.SampleBuffer
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread

actual object PlatformEnvironment {
    actual fun getAppDataDirectory(): String {
        val home = System.getProperty("user.home") ?: System.getProperty("java.io.tmpdir") ?: "/tmp"
        val dir = File(home, ".quran_cmp/quran_android")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath
    }

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}

actual class PlatformAudioPlayer actual constructor() {
    @Volatile
    private var playing = false

    @Volatile
    private var paused = false

    @Volatile
    private var stopRequested = false

    @Volatile
    private var currentPosMs = 0L

    @Volatile
    private var seekTargetMs = -1L

    @Volatile
    private var playbackSpeed = 1.0f

    private var currentSource: String? = null
    private var workerThread: Thread? = null

    actual fun play(
        fileOrUrl: String,
        startPositionMs: Long,
        speed: Float,
        onCompletion: () -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        currentSource = fileOrUrl
        playbackSpeed = speed.coerceIn(0.5f, 2.0f)
        currentPosMs = startPositionMs
        stopRequested = false
        paused = false
        playing = true

        workerThread = thread(start = true, isDaemon = true, name = "QuranDesktopAudioPlayer") {
            var line: SourceDataLine? = null
            var rawStream: InputStream? = null
            try {
                rawStream = openInputStream(fileOrUrl)
                var bitstream = Bitstream(BufferedInputStream(rawStream))
                var decoder = Decoder()

                // Fast-forward frames to startPositionMs if needed
                var accumulatedMs = 0f
                while (!stopRequested && accumulatedMs < startPositionMs) {
                    val header = bitstream.readFrame() ?: break
                    accumulatedMs += header.ms_per_frame()
                    bitstream.closeFrame()
                }
                currentPosMs = accumulatedMs.toLong()

                while (!stopRequested) {
                    if (seekTargetMs >= 0L) {
                        val target = seekTargetMs
                        seekTargetMs = -1L
                        try {
                            bitstream.close()
                        } catch (_: Exception) {
                        }
                        rawStream = openInputStream(fileOrUrl)
                        bitstream = Bitstream(BufferedInputStream(rawStream))
                        decoder = Decoder()
                        var skipMs = 0f
                        while (!stopRequested && skipMs < target) {
                            val h = bitstream.readFrame() ?: break
                            skipMs += h.ms_per_frame()
                            bitstream.closeFrame()
                        }
                        currentPosMs = skipMs.toLong()
                    }

                    if (paused) {
                        Thread.sleep(40)
                        continue
                    }

                    val header = bitstream.readFrame() ?: break
                    val output = decoder.decodeFrame(header, bitstream) as? SampleBuffer
                    if (output != null) {
                        if (line == null) {
                            val format = AudioFormat(
                                decoder.outputFrequency.toFloat(),
                                16,
                                decoder.outputChannels,
                                true,
                                false
                            )
                            val info = javax.sound.sampled.DataLine.Info(SourceDataLine::class.java, format)
                            if (AudioSystem.isLineSupported(info)) {
                                line = (AudioSystem.getLine(info) as SourceDataLine).apply {
                                    open(format)
                                    start()
                                }
                            }
                        }
                        val pcmShorts = output.buffer
                        val len = output.bufferLength
                        val step = playbackSpeed
                        if (line != null) {
                            val bytes = ByteArray(len * 2)
                            for (i in 0 until len) {
                                val s = pcmShorts[i].toInt()
                                bytes[i * 2] = (s and 0xff).toByte()
                                bytes[i * 2 + 1] = ((s shr 8) and 0xff).toByte()
                            }
                            line.write(bytes, 0, bytes.size)
                        } else {
                            // Headless fallback sleep matching frame duration
                            val sleepMs = (header.ms_per_frame() / step).toLong().coerceAtLeast(5L)
                            Thread.sleep(sleepMs)
                        }
                    }
                    accumulatedMs += header.ms_per_frame()
                    currentPosMs = accumulatedMs.toLong()
                    bitstream.closeFrame()
                }

                line?.drain()
                line?.close()
                bitstream.close()

                if (!stopRequested) {
                    playing = false
                    onCompletion()
                }
            } catch (e: Exception) {
                if (!stopRequested) {
                    playing = false
                    onError(e.message ?: "Desktop audio playback error")
                }
            } finally {
                try {
                    line?.close()
                } catch (_: Exception) {
                }
                try {
                    rawStream?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun openInputStream(fileOrUrl: String): InputStream {
        return if (fileOrUrl.startsWith("http://") || fileOrUrl.startsWith("https://")) {
            URI.create(fileOrUrl).toURL().openStream()
        } else {
            FileInputStream(File(fileOrUrl))
        }
    }

    actual fun pause() {
        paused = true
        playing = false
    }

    actual fun resume() {
        if (workerThread?.isAlive == true) {
            paused = false
            playing = true
        }
    }

    actual fun stop() {
        stopRequested = true
        paused = false
        playing = false
        workerThread?.interrupt()
        workerThread = null
    }

    actual fun seekTo(positionMs: Long) {
        seekTargetMs = positionMs.coerceAtLeast(0L)
        currentPosMs = seekTargetMs
    }

    actual fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2.0f)
    }

    actual fun getCurrentPositionMs(): Long = currentPosMs

    actual fun isPlaying(): Boolean = playing && !paused

    actual fun release() {
        stop()
    }
}

actual object PlatformShareService {
    actual fun copyToClipboard(text: String) {
        try {
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        } catch (_: Exception) {
        }
    }

    actual fun shareText(text: String, title: String) {
        copyToClipboard(text)
    }

    actual fun sharePngImage(pngBytes: ByteArray, fileName: String, textCaption: String): String {
        val dir = File(PlatformEnvironment.getAppDataDirectory(), "shared_cards")
        dir.mkdirs()
        val outFile = File(dir, fileName)
        outFile.writeBytes(pngBytes)
        copyToClipboard(textCaption)
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(outFile)
            }
        } catch (_: Exception) {
        }
        return outFile.absolutePath
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

