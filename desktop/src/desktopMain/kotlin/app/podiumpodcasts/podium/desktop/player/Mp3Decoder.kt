package app.podiumpodcasts.podium.desktop.player

import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.Header
import javazoom.jl.decoder.SampleBuffer
import java.io.BufferedInputStream
import java.io.InputStream

class Mp3Decoder : AudioDecoder {
    private lateinit var bitstream: Bitstream
    private lateinit var decoder: Decoder
    private var detectedBitrate: Int = 0
    override var sampleRate: Int = 44100; private set
    override var channels: Int = 2; private set
    override val bitsPerSample: Int = 16
    override val totalDurationMs: Long = 0L

    override fun open(inputStream: InputStream) {
        bitstream = Bitstream(BufferedInputStream(inputStream))
        decoder = Decoder()
        // Try to detect sample rate from first header
        val header = bitstream.readFrame()
        if (header != null) {
            sampleRate = header.frequency()
            channels = if (header.mode() == Header.SINGLE_CHANNEL) 1 else 2
            // Reset bitstream position (JLayer doesn't have reset, so we'll handle this differently)
            // We'll just use the detected values
        }
    }

    override fun readFrame(): ShortArray? {
        val header = bitstream.readFrame() ?: return null
        val buf = decoder.decodeFrame(header, bitstream) as? SampleBuffer ?: return null
        val len = buf.getBufferLength()
        return ShortArray(len) { buf.getBuffer()[it] }
    }

    override fun seekToMs(targetMs: Long) {
        // Approximate: ~26ms per MP3 frame at 44100Hz
        val framesToSkip = (targetMs / 26).toInt()
        repeat(framesToSkip) { bitstream.readFrame() }
    }

    override fun close() {
        runCatching { bitstream.close() }
    }
}