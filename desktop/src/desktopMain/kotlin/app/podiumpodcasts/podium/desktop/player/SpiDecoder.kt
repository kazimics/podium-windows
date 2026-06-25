package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import java.io.BufferedInputStream
import java.io.InputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

private const val TAG = "SpiDecoder"

class SpiDecoder : AudioDecoder {
    private var audioStream: javax.sound.sampled.AudioInputStream? = null
    private var jaadDecoder: JAADecAudioDecoder? = null
    override var sampleRate: Int = 44100; private set
    override var channels: Int = 2; private set
    override val bitsPerSample: Int = 16
    override val totalDurationMs: Long
        get() = jaadDecoder?.totalDurationMs ?: 0L

    override fun open(inputStream: InputStream) {
        Logger.i(TAG, "Opening decoder...")
        val buffered = BufferedInputStream(inputStream, 131072)
        buffered.mark(16)

        val header = ByteArray(12)
        val read = buffered.read(header)
        if (read < 8) {
            throw IllegalArgumentException("Stream too short to detect format")
        }

        val magic = String(header, 4, 4)
        Logger.i(TAG, "Format magic: $magic")

        when (magic) {
            "ftyp" -> {
                Logger.i(TAG, "Detected MP4/M4A format, using JAADec direct decoder")
                buffered.reset()
                openM4aViaJAADec(buffered)
            }
            "ID3" -> {
                Logger.i(TAG, "Detected MP3 format (ID3 tag)")
                buffered.reset()
                openAsMp3(buffered)
            }
            else -> {
                if (header[0] == 0xFF.toByte() && (header[1].toInt() and 0xE0) == 0xE0) {
                    Logger.i(TAG, "Detected MP3 format (sync word)")
                    buffered.reset()
                    openAsMp3(buffered)
                } else {
                    Logger.i(TAG, "Unknown format, trying SPI fallback")
                    buffered.reset()
                    openViaSpi(buffered)
                }
            }
        }
    }

    private fun openM4aViaJAADec(buffered: BufferedInputStream) {
        try {
            val decoder = JAADecAudioDecoder(buffered)
            decoder.open()
            jaadDecoder = decoder
            sampleRate = decoder.sampleRate
            channels = decoder.channels
            Logger.i(TAG, "JAADec decoder ready, sampleRate=$sampleRate, channels=$channels")
        } catch (e: Exception) {
            Logger.e(TAG, "JAADec failed: ${e.message}, trying SPI fallback")
            openViaSpi(buffered)
        }
    }

    private fun openAsMp3(buffered: BufferedInputStream) {
        try {
            val stream = AudioSystem.getAudioInputStream(buffered)
            Logger.i(TAG, "MP3 format accepted by SPI")
            val targetFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100f, 16, 2, 4, 44100f, false
            )
            audioStream = AudioSystem.getAudioInputStream(targetFormat, stream)
            sampleRate = 44100
            channels = 2
            Logger.i(TAG, "Decoder ready (MP3)")
        } catch (e: Exception) {
            Logger.e(TAG, "MP3 SPI failed: ${e.message}")
            throw IllegalArgumentException("Failed to decode MP3: ${e.message}")
        }
    }

    private fun openViaSpi(buffered: BufferedInputStream) {
        try {
            val stream = AudioSystem.getAudioInputStream(buffered)
            Logger.i(TAG, "SPI detected format: ${stream.format}")
            val targetFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100f, 16, 2, 4, 44100f, false
            )
            audioStream = AudioSystem.getAudioInputStream(targetFormat, stream)
            sampleRate = 44100
            channels = 2
            Logger.i(TAG, "Decoder ready (SPI)")
        } catch (e: Exception) {
            Logger.e(TAG, "SPI failed: ${e.message}")
            throw IllegalArgumentException("Unsupported audio format: ${e.message}")
        }
    }

    override fun readFrame(): ShortArray? {
        if (jaadDecoder != null) {
            return jaadDecoder!!.readFrame()
        }
        val frameSize = 4096
        val buf = ByteArray(frameSize * channels * 2)
        val bytesRead = audioStream?.read(buf) ?: return null
        if (bytesRead <= 0) return null
        val shorts = ShortArray(bytesRead / 2)
        for (i in shorts.indices) {
            shorts[i] = ((buf[i * 2 + 1].toInt() shl 8) or (buf[i * 2].toInt() and 0xFF)).toShort()
        }
        return shorts
    }

    override fun seekToMs(targetMs: Long) {
        if (jaadDecoder != null) {
            jaadDecoder!!.seekToMs(targetMs)
            return
        }
        val bytesToSkip = (targetMs.toLong() * 44100 * 4) / 1000
        audioStream?.skip(bytesToSkip)
    }

    override fun close() {
        jaadDecoder?.close()
        audioStream?.close()
    }
}
