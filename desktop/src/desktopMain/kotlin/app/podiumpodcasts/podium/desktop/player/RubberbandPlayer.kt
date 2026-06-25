package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.net.URL
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread

private const val TAG = "RubberbandPlayer"

class RubberbandPlayer {
    private var playerThread: Thread? = null
    private var isStopped = false
    private var currentSpeed = 1.0f
    private var decoder: AudioDecoder? = null
    private var stretcher: RubberbandStretcher? = null
    private var currentUrl: String? = null

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
        currentUrl = url
        isStopped = false
        onPositionChanged?.invoke(currentPosition)

        playerThread = thread(name = "rubberband-player", isDaemon = true) {
            try {
                Logger.i(TAG, "Thread started, loading DLLs...")
                RubberbandNativeLoader.load()

                Logger.i(TAG, "Creating decoder for: $url")
                val decoder = createDecoder(url)
                this.decoder = decoder

                Logger.i(TAG, "Opening input stream...")
                val inputStream = openInputStream(url)
                Logger.i(TAG, "Opening decoder...")
                decoder.open(inputStream)
                Logger.i(TAG, "Decoder opened, sampleRate=${decoder.sampleRate}, channels=${decoder.channels}")

                val sampleRate = decoder.sampleRate
                val channels = decoder.channels

                val audioFormat = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate.toFloat(), 16, channels,
                    channels * 2, sampleRate.toFloat(), false
                )
                val lineInfo = javax.sound.sampled.DataLine.Info(
                    SourceDataLine::class.java, audioFormat
                )
                Logger.i(TAG, "Getting audio line...")
                val line = AudioSystem.getLine(lineInfo) as SourceDataLine
                Logger.i(TAG, "Opening audio line...")
                line.open(audioFormat)
                line.start()
                Logger.i(TAG, "Audio line started")

                val options = RubberbandApi.RUBBERBAND_OPTION_PROCESS_REALTIME or
                    RubberbandApi.RUBBERBAND_OPTION_STRETCH_ELASTIC or
                    RubberbandApi.RUBBERBAND_OPTION_FORMANT_PRESERVED
                val stretcher = RubberbandStretcher(sampleRate, channels, options)
                this.stretcher = stretcher
                stretcher.setSpeed(speed)

                val prePad = stretcher.getPreferredStartPad()
                if (prePad > 0) {
                    val silence = ByteArray(prePad * channels * 2)
                    line.write(silence, 0, silence.size)
                }

                if (startPositionMs > 0) {
                    decoder.seekToMs(startPositionMs)
                }

                isPlaying = true
                onPlayStateChanged?.invoke(true)

                var totalInputSamples = 0L
                while (!isStopped) {
                    val frame = decoder.readFrame() ?: break

                    // Convert short[] to float[][] (interleaved → planar)
                    val sampleCount = frame.size / channels
                    val planarInput = Array(channels) { FloatArray(sampleCount) }
                    for (i in 0 until sampleCount) {
                        for (ch in 0 until channels) {
                            planarInput[ch][i] = frame[i * channels + ch].toFloat() / 32768f
                        }
                    }

                    stretcher.process(planarInput, sampleCount, false)

                    // Retrieve stretched output
                    val output = stretcher.retrieve()
                    if (output.isNotEmpty() && output[0].isNotEmpty()) {
                        // Convert planar float[][] → interleaved byte[]
                        val outSamples = output[0].size
                        val byteOut = ByteArray(outSamples * channels * 2)
                        for (i in 0 until outSamples) {
                            for (ch in 0 until channels) {
                                val sample = (output[ch][i] * 32767f).toInt().coerceIn(-32768, 32767)
                                val idx = (i * channels + ch) * 2
                                byteOut[idx] = (sample and 0xFF).toByte()
                                byteOut[idx + 1] = (sample shr 8).toByte()
                            }
                        }
                        line.write(byteOut, 0, byteOut.size)
                    }

                    totalInputSamples += sampleCount
                    currentPosition = (totalInputSamples * 1000L) / sampleRate
                    onPositionChanged?.invoke(currentPosition)
                }

                // Final flush
                stretcher.process(Array(channels) { floatArrayOf() }, 0, true)
                val remaining = stretcher.retrieve()
                if (remaining.isNotEmpty() && remaining[0].isNotEmpty()) {
                    val outSamples = remaining[0].size
                    val byteOut = ByteArray(outSamples * channels * 2)
                    for (i in 0 until outSamples) {
                        for (ch in 0 until channels) {
                            val sample = (remaining[ch][i] * 32767f).toInt().coerceIn(-32768, 32767)
                            val idx = (i * channels + ch) * 2
                            byteOut[idx] = (sample and 0xFF).toByte()
                            byteOut[idx + 1] = (sample shr 8).toByte()
                        }
                    }
                    line.write(byteOut, 0, byteOut.size)
                }

                line.drain()
                line.close()
                stretcher.release()
                decoder.close()

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

    private fun createDecoder(url: String): AudioDecoder {
        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.endsWith(".mp3") -> Mp3Decoder()
            lowerUrl.endsWith(".ogg") || lowerUrl.endsWith(".oga") || lowerUrl.endsWith(".opus") -> SpiDecoder()
            lowerUrl.endsWith(".flac") -> SpiDecoder()
            lowerUrl.endsWith(".wav") || lowerUrl.endsWith(".wave") -> SpiDecoder()
            lowerUrl.endsWith(".m4a") || lowerUrl.endsWith(".aac") || lowerUrl.endsWith(".mp4") -> SpiDecoder()
            else -> SpiDecoder() // Default: try SPI
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

    fun seek(positionMs: Long) {
        val url = currentUrl ?: return
        val wasPlaying = isPlaying
        stop()
        if (wasPlaying) {
            play(url, currentSpeed, positionMs)
        } else {
            currentPosition = positionMs
            onPositionChanged?.invoke(currentPosition)
        }
    }

    fun stop() {
        isStopped = true
        playerThread?.interrupt()
        playerThread = null
        val wasPlaying = isPlaying
        isPlaying = false
        if (wasPlaying) {
            onPlayStateChanged?.invoke(false)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        stretcher?.setSpeed(speed)
    }

    fun release() {
        stop()
        decoder?.close()
        stretcher?.release()
    }
}