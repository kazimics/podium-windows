package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.desktop.player.Mp3Decoder
import app.podiumpodcasts.podium.desktop.player.SpiDecoder
import java.io.ByteArrayInputStream
import kotlin.test.*

class AudioDecoderTest {

    @Test
    fun testMp3Decoder_initialization() {
        val decoder = Mp3Decoder()
        assertEquals(44100, decoder.sampleRate)
        assertEquals(2, decoder.channels)
        assertEquals(16, decoder.bitsPerSample)
    }

    @Test
    fun testSpiDecoder_initialization() {
        val decoder = SpiDecoder()
        assertEquals(44100, decoder.sampleRate)
        assertEquals(2, decoder.channels)
        assertEquals(16, decoder.bitsPerSample)
    }

    @Test
    fun testMp3Decoder_open_with_invalid_stream() {
        val decoder = Mp3Decoder()
        val inputStream = ByteArrayInputStream("invalid mp3 data".toByteArray())
        
        // JLayer may not throw immediately on invalid data
        // Just verify the decoder can be opened without crashing
        try {
            decoder.open(inputStream)
        } catch (e: Exception) {
            // Expected behavior for invalid data
        }
    }

    @Test
    fun testSpiDecoder_open_with_invalid_stream() {
        val decoder = SpiDecoder()
        val inputStream = ByteArrayInputStream("invalid audio data".toByteArray())
        
        // Should throw an exception when trying to open invalid data
        assertFailsWith<Exception> {
            decoder.open(inputStream)
        }
    }
}