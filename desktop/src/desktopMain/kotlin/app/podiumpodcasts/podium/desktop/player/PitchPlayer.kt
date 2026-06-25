package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.Header
import javazoom.jl.decoder.SampleBuffer
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.net.URL
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread

private const val TAG = "PitchPlayer"

@Deprecated("Replaced by RubberbandPlayer", ReplaceWith("RubberbandPlayer"))
class PitchPlayer {
    private var playerThread: Thread? = null
    private var isStopped = false
    private var currentSpeed = 1.0f
    private var seekTargetMs: Long? = null

    var isPlaying = false
        private set
    var currentPosition = 0L
        private set
    var onPlayStateChanged: ((Boolean) -> Unit)? = null
    var onPositionChanged: ((Long) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun play(url: String, speed: Float = 1.0f, startPositionMs: Long = 0L) {
        Logger.i(TAG, "play() url=$url, speed=$speed, startAt=${startPositionMs}ms")
        stop()
        currentSpeed = speed
        currentPosition = startPositionMs
        isStopped = false

        playerThread = thread(name = "pitch-player", isDaemon = true) {
            try {
                val inputStream = openInputStream(url)

                val bitstream = Bitstream(inputStream)
                val decoder = Decoder()
                val audioFormat = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100f, 16, 2, 4, 44100f, false
                )
                val lineInfo = javax.sound.sampled.DataLine.Info(SourceDataLine::class.java, audioFormat)
                val line = AudioSystem.getLine(lineInfo) as SourceDataLine
                line.open(audioFormat)
                line.start()

                val timeStretch = WsolaTimeStretch(44100, 2)

                isPlaying = true
                onPlayStateChanged?.invoke(true)

                if (startPositionMs > 0) {
                    skipToPosition(bitstream, decoder, startPositionMs)
                }

                var header: Header? = null
                while (!isStopped) {
                    header = bitstream.readFrame() ?: break
                    val sampleBuffer = decoder.decodeFrame(header, bitstream) as? SampleBuffer ?: continue

                    val sampleCount = sampleBuffer.getBufferLength()
                    if (sampleCount > 0) {
                        val rawBuffer = sampleBuffer.getBuffer()
                        val pcmData = ShortArray(sampleCount)
                        for (i in 0 until sampleCount) {
                            pcmData[i] = rawBuffer[i]
                        }

                        val output = if (currentSpeed != 1.0f) {
                            timeStretch.process(pcmData, currentSpeed)
                        } else {
                            pcmData
                        }

                        val byteOutput = ByteArray(output.size * 2)
                        for (i in output.indices) {
                            byteOutput[i * 2] = (output[i].toInt() and 0xFF).toByte()
                            byteOutput[i * 2 + 1] = (output[i].toInt() shr 8).toByte()
                        }

                        checkSeek()
                        line.write(byteOutput, 0, byteOutput.size)

                        val bytesPlayed = sampleCount * 2L
                        currentPosition += (bytesPlayed * 1000L) / (44100L * 2)
                        onPositionChanged?.invoke(currentPosition)
                    }
                }

                line.drain()
                line.close()
                bitstream.close()

                if (!isStopped) {
                    isPlaying = false
                    onPlayStateChanged?.invoke(false)
                }
            } catch (e: Exception) {
                if (!isStopped) {
                    Logger.e(TAG, "Playback error: ${e.message}")
                    onError?.invoke("Playback failed: ${e.message}")
                    isPlaying = false
                    onPlayStateChanged?.invoke(false)
                }
            }
        }
    }

    private fun openInputStream(url: String): InputStream {
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            BufferedInputStream(connection.getInputStream())
        } else {
            val file = File(url)
            if (!file.exists()) throw java.io.FileNotFoundException("File not found: $url")
            BufferedInputStream(file.inputStream())
        }
    }

    private fun skipToPosition(bitstream: Bitstream, decoder: Decoder, targetMs: Long) {
        Logger.d(TAG, "Skipping to ${targetMs}ms")
        var accumulatedMs = 0L
        while (accumulatedMs < targetMs && !isStopped) {
            val header = bitstream.readFrame() ?: break
            decoder.decodeFrame(header, bitstream)
            accumulatedMs += 26L
        }
        currentPosition = targetMs
        Logger.d(TAG, "Skipped to ${accumulatedMs}ms (target=${targetMs}ms)")
    }

    private fun checkSeek() {
        val target = seekTargetMs
        if (target != null) {
            seekTargetMs = null
            currentPosition = target
            Logger.d(TAG, "Seek to ${target}ms (thread restart needed for accurate seek)")
        }
    }

    fun seek(positionMs: Long) {
        seekTargetMs = positionMs
    }

    fun stop() {
        isStopped = true
        playerThread?.interrupt()
        playerThread = null
        isPlaying = false
    }

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
    }

    fun release() {
        stop()
    }
}
