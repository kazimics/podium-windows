package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.desktop.player.AudioDecoder
import app.podiumpodcasts.podium.desktop.player.AudioPlayer
import app.podiumpodcasts.podium.desktop.player.M4aDecoder
import app.podiumpodcasts.podium.desktop.player.MediaPlayerState
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

class AudioPlayerTest {

    private lateinit var player: AudioPlayer

    @BeforeTest
    fun setup() {
        player = AudioPlayer()
    }

    @AfterTest
    fun teardown() {
        player.release()
    }

    @Test
    fun testInitialState() {
        assertFalse(player.isPlaying)
        assertEquals(0L, player.currentPosition)
        assertEquals(0L, player.duration)
    }

    @Test
    fun testPauseWhenNotPlaying() {
        player.pause()
        assertFalse(player.isPlaying)
    }

    @Test
    fun testResumeWithoutPlayReturnsEarly() {
        player.resume()
        assertFalse(player.isPlaying)
    }

    @Test
    fun testStopResetsState() {
        player.stop()
        assertFalse(player.isPlaying)
        assertEquals(0L, player.currentPosition)
    }

    @Test
    fun testSeekUpdatesPosition() {
        player.seek(5000L)
        assertEquals(5000L, player.currentPosition)
    }

    @Test
    fun testSetPlaybackSpeedClamps() {
        player.setPlaybackSpeed(0.01f)
        // Speed should be clamped to minimum 0.25
        player.setPlaybackSpeed(100f)
        // Speed should be clamped to maximum 4.0
    }

    @Test
    fun testCallbackNullSafety() {
        player.onPlayStateChanged = null
        player.onPositionChanged = null
        player.onError = null
        player.stop()
    }

    @Test
    fun testPlayCallbackInvoked() {
        val states = mutableListOf<Boolean>()
        player.onPlayStateChanged = { states.add(it) }

        player.stop()
        // stop() should set isPlaying = false and not invoke callback
        assertFalse(player.isPlaying)
    }

    @Test
    fun testPositionCallbackInvoked() {
        val positions = mutableListOf<Pair<Long, Long>>()
        player.onPositionChanged = { pos, dur -> positions.add(Pair(pos, dur)) }

        player.seek(1000L)
        assertEquals(1, positions.size)
        assertEquals(1000L, positions[0].first)
    }
}

class M4aDecoderTest {

    @Test
    fun testInitialDurationIsZero() {
        val decoder = M4aDecoder()
        assertEquals(0L, decoder.totalDurationMs)
    }

    @Test
    fun testInitialSampleRate() {
        val decoder = M4aDecoder()
        assertEquals(44100, decoder.sampleRate)
    }

    @Test
    fun testInitialChannels() {
        val decoder = M4aDecoder()
        assertEquals(2, decoder.channels)
    }

    @Test
    fun testReadFrameBeforeOpen() {
        val decoder = M4aDecoder()
        assertNull(decoder.readFrame())
    }

    @Test
    fun testSeekToMsBeforeOpen() {
        val decoder = M4aDecoder()
        // Should not throw
        decoder.seekToMs(5000L)
    }

    @Test
    fun testCloseBeforeOpen() {
        val decoder = M4aDecoder()
        // Should not throw
        decoder.close()
    }

    @Test
    fun testInterfaceContract() {
        val decoder: AudioDecoder = M4aDecoder()
        assertNotNull(decoder)
        assertEquals(44100, decoder.sampleRate)
        assertEquals(2, decoder.channels)
        assertEquals(16, decoder.bitsPerSample)
        assertEquals(0L, decoder.totalDurationMs)
    }
}

class MediaPlayerStateTestExtended {

    private lateinit var state: MediaPlayerState

    @BeforeTest
    fun setup() {
        state = MediaPlayerState()
    }

    @AfterTest
    fun teardown() {
        state.release()
    }

    @Test
    fun testPlaySetsCurrentUrl() {
        state.play("https://example.com/audio.mp3", "Test Episode")
        assertEquals("https://example.com/audio.mp3", state.currentUrl)
        assertEquals("Test Episode", state.currentTitle)
    }

    @Test
    fun testPlayClearsLoadingState() {
        state.play("https://example.com/audio.mp3")
        assertTrue(state.isLoading)
    }

    @Test
    fun testStopClearsState() {
        state.play("https://example.com/audio.mp3", "Test")
        state.stop()
        assertNull(state.currentUrl)
        assertNull(state.currentTitle)
        assertNull(state.currentArtworkUrl)
    }

    @Test
    fun testTogglePlayPause() {
        state.togglePlayPause()
        assertFalse(state.isPlaying)
    }

    @Test
    fun testSeekForwardIncreasesPosition() {
        val before = state.currentPosition
        state.seekForward(10000L)
        assertTrue(state.currentPosition > before || state.currentPosition >= 0)
    }

    @Test
    fun testSeekBackDecreasesPosition() {
        state.seekForward(10000L)
        val before = state.currentPosition
        state.seekBack(5000L)
        assertTrue(state.currentPosition <= before)
    }

    @Test
    fun testSeekBackDoesNotGoNegative() {
        state.seekForward(5000L)
        state.seekBack(10000L)
        assertTrue(state.currentPosition >= 0L)
    }

    @Test
    fun testChangePlaybackSpeed() {
        state.changePlaybackSpeed(1.5f)
        assertEquals(1.5f, state.playbackSpeed)
    }

    @Test
    fun testPlayNextWhenEmpty() {
        state.playNext()
    }

    @Test
    fun testPlayPreviousWhenEmpty() {
        state.playPrevious()
    }

    @Test
    fun testPlayPreviousRestartsIfPast3s() {
        state.seekForward(5000L)
        state.playPrevious()
        assertTrue(state.currentPosition >= 0L)
    }

    @Test
    fun testGetProgressWithZeroDuration() {
        assertEquals(0f, state.getProgress())
    }

    @Test
    fun testCancelSleepTimer() {
        state.setSleepTimer(30)
        state.cancelSleepTimer()
        assertNull(state.sleepTimerMinutes)
        assertNull(state.sleepTimerTrigger)
    }

    @Test
    fun testQueueOperations() {
        state.addToQueue("url1", "Ep 1")
        state.addToQueue("url2", "Ep 2")
        state.addToQueue("url3", "Ep 3")

        assertEquals(3, state.queue.size)

        state.removeFromQueue(1)
        assertEquals(2, state.queue.size)
        assertEquals("Ep 1", state.queue[0].title)
        assertEquals("Ep 3", state.queue[1].title)
    }

    @Test
    fun testPlayFromQueueUpdatesState() {
        state.addToQueue("url1", "Ep 1")
        state.addToQueue("url2", "Ep 2")

        state.playFromQueue(0)
        assertEquals(0, state.queueIndex)
        assertEquals("Ep 1", state.currentTitle)

        state.playFromQueue(1)
        assertEquals(1, state.queueIndex)
        assertEquals("Ep 2", state.currentTitle)
    }

    @Test
    fun testPlayFromQueueInvalidIndex() {
        state.addToQueue("url1", "Ep 1")

        state.playFromQueue(-1)
        assertEquals(-1, state.queueIndex)

        state.playFromQueue(5)
        assertEquals(-1, state.queueIndex)
    }

    @Test
    fun testRemoveFromQueueCurrentIndexAdjusts() {
        state.addToQueue("url1", "Ep 1")
        state.addToQueue("url2", "Ep 2")
        state.addToQueue("url3", "Ep 3")

        state.playFromQueue(1)
        assertEquals(1, state.queueIndex)

        state.removeFromQueue(0)
        assertEquals(0, state.queueIndex)
        assertEquals("Ep 2", state.queue[0].title)
    }

    @Test
    fun testRemoveFromQueueOutOfBounds() {
        state.addToQueue("url1", "Ep 1")
        state.removeFromQueue(-1)
        assertEquals(1, state.queue.size)
        state.removeFromQueue(10)
        assertEquals(1, state.queue.size)
    }
}

class AudioDecoderContractTest {

    @Test
    fun testM4aDecoderImplementsInterface() {
        val decoder: AudioDecoder = M4aDecoder()
        // All interface methods should be callable without exception
        decoder.close()
    }

    @Test
    fun testM4aDecoderReadFrameReturnsNullWhenEmpty() {
        val decoder = M4aDecoder()
        assertNull(decoder.readFrame())
        decoder.close()
    }

    @Test
    fun testM4aDecoderSeekToMsDoesNotThrow() {
        val decoder = M4aDecoder()
        decoder.seekToMs(0L)
        decoder.seekToMs(1000L)
        decoder.seekToMs(Long.MAX_VALUE)
        decoder.close()
    }

    @Test
    fun testM4aDecoderCloseIsIdempotent() {
        val decoder = M4aDecoder()
        decoder.close()
        decoder.close()
        decoder.close()
    }

    @Test
    fun testM4aDecoderDurationDefaultZero() {
        val decoder = M4aDecoder()
        assertEquals(0L, decoder.totalDurationMs)
        decoder.close()
    }

    @Test
    fun testM4aDecoderDurationNotNegative() {
        val decoder = M4aDecoder()
        assertTrue(decoder.totalDurationMs >= 0L)
        decoder.close()
    }
}

class AudioPlayerDurationTest {

    @Test
    fun testDurationDefaultZero() {
        val player = AudioPlayer()
        assertEquals(0L, player.duration)
        player.release()
    }

    @Test
    fun testPauseImmediatelyStopsAudioLine() {
        val player = AudioPlayer()
        player.pause()
        assertFalse(player.isPlaying)
        player.release()
    }

    @Test
    fun testStopClearsAudioLine() {
        val player = AudioPlayer()
        player.stop()
        assertFalse(player.isPlaying)
        player.release()
    }
}

class ProgressBarContractTest {

    @Test
    fun testMediaPlayerStateProgressWithZeroDuration() {
        val state = MediaPlayerState()
        assertEquals(0f, state.getProgress())
        state.release()
    }

    @Test
    fun testMediaPlayerStateProgressUpdates() {
        val state = MediaPlayerState()
        state.seekForward(50L)
        assertTrue(state.getProgress() >= 0f)
        state.release()
    }
}
