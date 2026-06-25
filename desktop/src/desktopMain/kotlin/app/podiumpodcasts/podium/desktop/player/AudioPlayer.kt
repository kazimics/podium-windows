package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.PushbackInputStream
import java.net.URL
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread

private const val TAG = "AudioPlayer"

class AudioPlayer {

    private var playerThread: Thread? = null
    @Volatile private var isStopped = false
    private var currentSpeed = 1.0f
    private var decoder: AudioDecoder? = null
    private var stretcher: RubberbandStretcher? = null
    private var audioLine: javax.sound.sampled.SourceDataLine? = null
    private var currentUrl: String? = null
    @Volatile var currentVolume = 100; private set

    @Volatile var isPlaying = false; private set
    @Volatile var currentPosition = 0L; private set
    var duration = 0L
    private var isUserPaused = false

    var onPlayStateChanged: ((Boolean) -> Unit)? = null
    var onPositionChanged: ((Long, Long) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun play(url: String, speed: Float = 1.0f, startPositionMs: Long = 0L, durationMs: Long = 0L) {
        Logger.i(TAG, "play() url=$url, speed=$speed, startAt=${startPositionMs}ms, durationMs=$durationMs")
        isUserPaused = false
        stop()
        currentSpeed = speed
        currentPosition = startPositionMs
        currentUrl = url
        isStopped = false
        if (durationMs > 0) {
            duration = durationMs
        }
        onPositionChanged?.invoke(currentPosition, duration)

        playerThread = thread(name = "audio-player", isDaemon = true) {
            try {
                RubberbandNativeLoader.load()

                val rawStream = openInputStream(url)
                val pushback = PushbackInputStream(rawStream, 16)

                val header = ByteArray(12)
                val read = pushback.read(header)
                if (read < 8) throw IllegalArgumentException("Stream too short")
                pushback.unread(header, 0, read)

                val magic = String(header, 4, 4)
                Logger.i(TAG, "Format: $magic")

                val dec = when {
                    magic == "ftyp" -> {
                        Logger.i(TAG, "Using M4aDecoder")
                        M4aDecoder()
                    }
                    magic == "ID3" || (header[0] == 0xFF.toByte() && (header[1].toInt() and 0xE0) == 0xE0) -> {
                        Logger.i(TAG, "Using Mp3Decoder")
                        Mp3Decoder()
                    }
                    else -> {
                        Logger.i(TAG, "Unknown format, trying M4aDecoder")
                        M4aDecoder()
                    }
                }
                decoder = dec

                if (dec is M4aDecoder) {
                    dec.openUrl(url, startPositionMs)
                } else {
                    dec.open(pushback)
                }

                if (dec.totalDurationMs > 0) {
                    duration = dec.totalDurationMs
                }

                startPlaybackLoop(dec, speed, startPositionMs)
            } catch (e: Exception) {
                Logger.e(TAG, "Playback error: ${e.message}")
                if (!isStopped) {
                    onError?.invoke("Playback failed: ${e.message}")
                    isPlaying = false
                    onPlayStateChanged?.invoke(false)
                }
            }
        }
    }

    private fun startPlaybackLoop(dec: AudioDecoder, speed: Float, startPositionMs: Long) {
        val sampleRate = dec.sampleRate
        val ch = dec.channels

        val audioFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate.toFloat(), 16, ch,
            ch * 2, sampleRate.toFloat(), false
        )
        val lineInfo = javax.sound.sampled.DataLine.Info(SourceDataLine::class.java, audioFormat)
        val line = AudioSystem.getLine(lineInfo) as SourceDataLine
        audioLine = line
        line.open(audioFormat)
        line.start()

        val options = RubberbandApi.RUBBERBAND_OPTION_PROCESS_REALTIME or
            RubberbandApi.RUBBERBAND_OPTION_STRETCH_ELASTIC or
            RubberbandApi.RUBBERBAND_OPTION_FORMANT_PRESERVED
        val str = RubberbandStretcher(sampleRate, ch, options)
        stretcher = str
        str.setSpeed(speed)

        val prePad = str.getPreferredStartPad()
        if (prePad > 0) {
            try { line.write(ByteArray(prePad * ch * 2), 0, prePad * ch * 2) } catch (_: Exception) {}
        }

        if (startPositionMs > 0) {
            dec.seekToMs(startPositionMs)
        }

        isPlaying = true
        onPlayStateChanged?.invoke(true)

        val invScale = 1.0f / 32768f
        val scale = 32767f
        val maxFrameSize = 8192
        val planar = Array(ch) { FloatArray(maxFrameSize) }
        val byteOut = ByteArray(maxFrameSize * ch * 2)
        val emptyPlanar = Array(ch) { floatArrayOf() }
        var lastPositionCallbackNanos = System.nanoTime()

        var totalInputSamples = startPositionMs * sampleRate / 1000L
        while (!isStopped) {
            val frame = dec.readFrame() ?: break

            val sampleCount = frame.size / ch
            for (i in 0 until sampleCount) {
                for (c in 0 until ch) {
                    planar[c][i] = frame[i * ch + c] * invScale
                }
            }

            str.process(planar, sampleCount, false)
            val output = str.retrieve()

            if (output.isNotEmpty() && output[0].isNotEmpty()) {
                val outSamples = output[0].size
                if (outSamples <= 0) continue
                val volumeFactor = currentVolume / 100f
                val outBytes = outSamples * ch * 2
                for (i in 0 until outSamples) {
                    for (c in 0 until ch) {
                        val s = (output[c][i] * scale * volumeFactor).toInt().coerceIn(-32768, 32767)
                        val idx = (i * ch + c) * 2
                        byteOut[idx] = (s and 0xFF).toByte()
                        byteOut[idx + 1] = (s shr 8).toByte()
                    }
                }
                try { line.write(byteOut, 0, outBytes) } catch (_: Exception) { break }
            }

            totalInputSamples += sampleCount
            currentPosition = ((totalInputSamples * 1000L) / sampleRate).coerceAtMost(duration)
            val now = System.nanoTime()
            if (now - lastPositionCallbackNanos >= 300_000_000L) {
                onPositionChanged?.invoke(currentPosition, duration)
                lastPositionCallbackNanos = now
            }
        }

        str.process(emptyPlanar, 0, true)
        val remaining = str.retrieve()
        if (remaining.isNotEmpty() && remaining[0].isNotEmpty()) {
            val outSamples = remaining[0].size
            val volumeFactor = currentVolume / 100f
            val outBytes = outSamples * ch * 2
            for (i in 0 until outSamples) {
                for (c in 0 until ch) {
                    val s = (remaining[c][i] * scale * volumeFactor).toInt().coerceIn(-32768, 32767)
                    val idx = (i * ch + c) * 2
                    byteOut[idx] = (s and 0xFF).toByte()
                    byteOut[idx + 1] = (s shr 8).toByte()
                }
            }
            line.write(byteOut, 0, outBytes)
        }

        try { line.drain() } catch (_: Exception) {}
        try { line.close() } catch (_: Exception) {}
        audioLine = null
        str.release()

        if (!isStopped) {
            currentPosition = duration
            onPositionChanged?.invoke(currentPosition, duration)
            isPlaying = false
            onPlayStateChanged?.invoke(false)
        }
    }

    private fun openInputStream(url: String): InputStream {
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            val conn = URL(url).openConnection()
            conn.connectTimeout = 10000
            conn.readTimeout = 30000
            BufferedInputStream(conn.getInputStream(), 65536)
        } else {
            val file = File(url)
            if (!file.exists()) throw java.io.FileNotFoundException("File not found: $url")
            BufferedInputStream(file.inputStream(), 65536)
        }
    }

    fun pause() {
        Logger.d(TAG, "pause()")
        isUserPaused = true
        stop()
        isPlaying = false
        onPlayStateChanged?.invoke(false)
    }

    fun resume() {
        Logger.d(TAG, "resume()")
        isUserPaused = false
        val url = currentUrl ?: return
        val pos = currentPosition
        val existingDecoder = decoder
        if (existingDecoder != null && existingDecoder !is M4aDecoder) {
            Logger.d(TAG, "Reusing existing decoder, seeking to ${pos}ms")
            isStopped = false
            existingDecoder.seekToMs(pos)
            currentPosition = pos
            onPositionChanged?.invoke(currentPosition, duration)
            playerThread = thread(name = "audio-player-resume", isDaemon = true) {
                try {
                    RubberbandNativeLoader.load()
                    startPlaybackLoop(existingDecoder, currentSpeed, pos)
                } catch (e: Exception) {
                    Logger.e(TAG, "Playback error: ${e.message}")
                    if (!isStopped) {
                        onError?.invoke("Playback failed: ${e.message}")
                        isPlaying = false
                        onPlayStateChanged?.invoke(false)
                    }
                }
            }
        } else {
            decoder?.close()
            decoder = null
            play(url, currentSpeed, pos, duration)
        }
    }

    fun stop() {
        isStopped = true
        playerThread?.interrupt()
        playerThread = null
        isPlaying = false
        try { audioLine?.close() } catch (_: Exception) {}
        audioLine = null
    }

    fun seek(positionMs: Long) {
        currentPosition = positionMs
        onPositionChanged?.invoke(currentPosition, duration)
        val url = currentUrl ?: return
        stop()
        if (!isUserPaused) {
            play(url, currentSpeed, positionMs)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        Logger.d(TAG, "setPlaybackSpeed($clamped)")
        currentSpeed = clamped
        stretcher?.setSpeed(clamped)
    }

    fun setVolume(vol: Int) {
        currentVolume = vol.coerceIn(0, 100)
    }

    fun release() {
        stop()
        decoder?.close()
        stretcher?.release()
        decoder = null
        stretcher = null
        currentUrl = null
    }
}
