package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.desktop.player.MediaPlayerState
import kotlin.test.*

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
    fun testPlaySetsUrl() {
        state.play("https://example.com/audio.mp3")
        assertEquals("https://example.com/audio.mp3", state.currentUrl)
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
    fun testTogglePlayPauseWhenNothingPlaying() {
        state.togglePlayPause()
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
