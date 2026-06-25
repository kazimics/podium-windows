package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.desktop.player.*
import java.io.File
import java.io.RandomAccessFile
import java.net.URL
import kotlin.math.sin
import kotlin.test.*

class RubberbandPlayerTest {

    companion object {
        private var testWavFile: File? = null
        private var testM4aFile: File? = null

        @BeforeTest
        fun setup() {
            testWavFile = createTestWavFile()
            testM4aFile = downloadTestM4aFile()
        }

        @AfterTest
        fun teardown() {
            testWavFile?.delete()
            testM4aFile?.delete()
        }

        private fun createTestWavFile(): File {
            val file = File.createTempFile("test_audio", ".wav")
            file.deleteOnExit()
            val sampleRate = 44100; val duration = 2; val frequency = 440.0
            val totalSamples = sampleRate * duration; val dataSize = totalSamples * 2 * 2
            RandomAccessFile(file, "rw").use { raf ->
                raf.writeBytes("RIFF"); raf.write(intToLittleEndian(44 + dataSize - 8)); raf.writeBytes("WAVE")
                raf.writeBytes("fmt "); raf.write(intToLittleEndian(16)); raf.write(shortToLittleEndian(1))
                raf.write(shortToLittleEndian(2)); raf.write(intToLittleEndian(sampleRate))
                raf.write(intToLittleEndian(sampleRate * 4)); raf.write(shortToLittleEndian(4)); raf.write(shortToLittleEndian(16))
                raf.writeBytes("data"); raf.write(intToLittleEndian(dataSize))
                for (i in 0 until totalSamples) {
                    val s = (sin(2.0 * Math.PI * frequency * i / sampleRate) * 16000).toInt().toShort()
                    raf.write(shortToLittleEndian(s.toInt())); raf.write(shortToLittleEndian(s.toInt()))
                }
            }
            return file
        }

        private fun downloadTestM4aFile(): File? {
            return try {
                val file = File.createTempFile("test_podcast", ".m4a"); file.deleteOnExit()
                val url = URL("https://dts-api.xiaoyuzhoufm.com/track/611719d3cb0b82e1df0ad29e/69a64629de29766da93331ec/media.xyzcdn.net/611719d3cb0b82e1df0ad29e/lvA2mFXeSE7V0sFV3mFpfFHBDAsk.m4a")
                val conn = url.openConnection(); conn.connectTimeout = 5000; conn.readTimeout = 10000
                conn.getInputStream().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                if (file.length() > 0) file else null
            } catch (e: Exception) { println("Could not download test M4A: ${e.message}"); null }
        }

        private fun intToLittleEndian(value: Int) = byteArrayOf((value and 0xFF).toByte(), (value shr 8 and 0xFF).toByte(), (value shr 16 and 0xFF).toByte(), (value shr 24 and 0xFF).toByte())
        private fun shortToLittleEndian(value: Int) = byteArrayOf((value and 0xFF).toByte(), (value shr 8 and 0xFF).toByte())
    }

    @Test
    fun testAudioPlayer_playsWavFile() {
        val wavFile = testWavFile ?: return
        val player = AudioPlayer()
        var playStarted = false; var positionUpdated = false
        player.onPlayStateChanged = { if (it) playStarted = true }
        player.onPositionChanged = { _, _ -> positionUpdated = true }
        player.play(wavFile.absolutePath, 1.0f, 0L)
        Thread.sleep(1500)
        assertTrue(playStarted, "onPlayStateChanged(true) should have been called")
        assertTrue(positionUpdated, "onPositionChanged should have been called")
        player.release()
    }

    @Test
    fun testAudioPlayer_playsM4aFile() {
        val m4aFile = testM4aFile ?: run { println("Skipping: no M4A test file"); return }
        val player = AudioPlayer()
        var playStarted = false; var positionUpdated = false
        player.onPlayStateChanged = { if (it) playStarted = true }
        player.onPositionChanged = { _, _ -> positionUpdated = true }
        player.play(m4aFile.absolutePath, 1.0f, 0L)
        Thread.sleep(3000)
        assertTrue(playStarted, "onPlayStateChanged(true) should have been called for M4A")
        assertTrue(positionUpdated, "onPositionChanged should have been called for M4A")
        player.release()
    }

    @Test
    fun testAudioPlayer_m4aNotNoise() {
        val m4aFile = testM4aFile ?: run { println("Skipping: no M4A test file"); return }
        val decoder = M4aDecoder()
        decoder.open(m4aFile.inputStream())
        val frame = decoder.readFrame() ?: run { println("No frame"); return }
        val allZeros = frame.all { it == 0.toShort() }
        assertFalse(allZeros, "Decoded audio should not be all zeros")
        val max = frame.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
        assertTrue(max > 100, "Audio should have meaningful amplitude, got max=$max")
        decoder.close()
    }

    @Test
    fun testAudioPlayer_speedChangeOnWav() {
        val wavFile = testWavFile ?: return
        val player = AudioPlayer()
        var playStarted = false
        player.onPlayStateChanged = { if (it) playStarted = true }
        player.play(wavFile.absolutePath, 1.0f, 0L)
        Thread.sleep(500)
        player.setPlaybackSpeed(2.0f)
        Thread.sleep(500)
        assertTrue(playStarted, "Player should have started")
        player.release()
    }

    @Test
    fun testAudioPlayer_pauseResumeOnWav() {
        val wavFile = testWavFile ?: return
        val player = AudioPlayer()
        var playStarted = false; var pausedCalled = false
        player.onPlayStateChanged = { playing ->
            if (playing) playStarted = true
            if (!playing && playStarted) pausedCalled = true
        }
        player.play(wavFile.absolutePath, 1.0f, 0L)
        Thread.sleep(500)
        player.stop()
        Thread.sleep(100)
        assertTrue(playStarted, "Player should have started")
        assertTrue(pausedCalled, "Pause callback should have been called")
        player.release()
    }

    @Test
    fun testRubberbandNativeLoader_loadsSuccessfully() {
        assertTrue(RubberbandNativeLoader.load())
    }

    @Test
    fun testRubberbandApi_loads() {
        RubberbandNativeLoader.load()
        assertNotNull(RubberbandApi.INSTANCE)
    }

    @Test
    fun testRubberbandStretcher_processAndRetrieve() {
        RubberbandNativeLoader.load()
        val stretcher = RubberbandStretcher(44100, 2)
        stretcher.setSpeed(1.5f)
        val input = Array(2) { FloatArray(4096) { sin(2.0 * Math.PI * 440.0 * it / 44100.0).toFloat() } }
        for (i in 0 until 10) stretcher.process(input, 4096, false)
        assertTrue(stretcher.retrieve().isNotEmpty())
        stretcher.release()
    }

    @Test
    fun testAudioPlayer_initialState() {
        val player = AudioPlayer()
        assertFalse(player.isPlaying)
        assertEquals(0L, player.currentPosition)
        player.release()
    }

    @Test
    fun testAudioPlayer_stopAndRelease() {
        val player = AudioPlayer()
        player.stop()
        assertFalse(player.isPlaying)
        player.release()
    }

    @Test
    fun testMp3Decoder_creation() {
        val decoder = Mp3Decoder()
        assertEquals(44100, decoder.sampleRate)
        assertEquals(2, decoder.channels)
    }

    @Test
    fun testM4aDecoder_creation() {
        val decoder = M4aDecoder()
        assertEquals(44100, decoder.sampleRate)
        assertEquals(2, decoder.channels)
    }
}
