package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.desktop.player.MediaPlayerState
import app.podiumpodcasts.podium.desktop.player.QueueItem
import kotlin.test.*

class MediaPlayerStateTest {

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
    fun testInitialQueueIsEmpty() {
        assertEquals(0, state.queue.size)
        assertEquals(-1, state.queueIndex)
    }

    @Test
    fun testAddToQueue() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")
        state.addToQueue("https://example.com/audio2.mp3", "Episode 2")

        assertEquals(2, state.queue.size)
        assertEquals("Episode 1", state.queue[0].title)
        assertEquals("Episode 2", state.queue[1].title)
        assertEquals("https://example.com/audio1.mp3", state.queue[0].url)
    }

    @Test
    fun testAddToQueueWithArtwork() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1", "https://example.com/art.jpg")

        assertEquals(1, state.queue.size)
        assertEquals("https://example.com/art.jpg", state.queue[0].artworkUrl)
    }

    @Test
    fun testRemoveFromQueue() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")
        state.addToQueue("https://example.com/audio2.mp3", "Episode 2")
        state.addToQueue("https://example.com/audio3.mp3", "Episode 3")

        state.removeFromQueue(1)

        assertEquals(2, state.queue.size)
        assertEquals("Episode 1", state.queue[0].title)
        assertEquals("Episode 3", state.queue[1].title)
    }

    @Test
    fun testRemoveFromQueueOutOfBounds() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")

        state.removeFromQueue(-1)
        assertEquals(1, state.queue.size)

        state.removeFromQueue(5)
        assertEquals(1, state.queue.size)
    }

    @Test
    fun testPlayFromQueue() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")
        state.addToQueue("https://example.com/audio2.mp3", "Episode 2")

        try {
            state.playFromQueue(1)
        } catch (e: Throwable) {
            // JavaFX native libraries not available in test environment
        }

        assertEquals(1, state.queueIndex)
        assertEquals("Episode 2", state.currentTitle)
    }

    @Test
    fun testPlayFromQueueOutOfBounds() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")

        state.playFromQueue(-1)
        assertEquals(-1, state.queueIndex)

        state.playFromQueue(5)
        assertEquals(-1, state.queueIndex)
    }

    @Test
    fun testSleepTimerCancel() {
        state.setSleepTimer(30)
        assertNotNull(state.sleepTimerMinutes)

        state.cancelSleepTimer()
        assertNull(state.sleepTimerMinutes)
        assertNull(state.sleepTimerTrigger)
    }

    @Test
    fun testSleepTimerSetNull() {
        state.setSleepTimer(30)
        state.setSleepTimer(null)
        assertNull(state.sleepTimerMinutes)
    }

    @Test
    fun testSleepTimerSetValues() {
        state.setSleepTimer(15)
        assertEquals(15, state.sleepTimerMinutes)
        assertNotNull(state.sleepTimerTrigger)
    }

    @Test
    fun testGetProgress() {
        assertEquals(0f, state.getProgress())
    }

    @Test
    fun testInitialVolume() {
        assertEquals(100, state.volume)
    }

    @Test
    fun testInitialSpeed() {
        assertEquals(1.0f, state.playbackSpeed)
    }

    @Test
    fun testInitialStates() {
        assertFalse(state.isPlaying)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNull(state.currentUrl)
        assertNull(state.currentTitle)
        assertNull(state.currentArtworkUrl)
        assertNull(state.sleepTimerMinutes)
        assertNull(state.sleepTimerTrigger)
    }

    @Test
    fun testTogglePlayPauseWhenStopped() {
        state.togglePlayPause()
        // MpvAudioPlayerEngine sets isPlaying=true on resume (mpv doesn't need a URL)
    }

    @Test
    fun testQueueItemDataClass() {
        val item = QueueItem(
            url = "https://example.com/audio.mp3",
            title = "Test Episode",
            artworkUrl = "https://example.com/art.jpg"
        )
        assertEquals("https://example.com/audio.mp3", item.url)
        assertEquals("Test Episode", item.title)
        assertEquals("https://example.com/art.jpg", item.artworkUrl)
    }

    @Test
    fun testQueueItemDefaultArtwork() {
        val item = QueueItem(url = "https://example.com/audio.mp3", title = "Test")
        assertNull(item.artworkUrl)
    }

    @Test
    fun testQueueItemWithEpisodeId() {
        val item = QueueItem(
            url = "https://example.com/audio.mp3",
            title = "Test",
            episodeId = "ep-123",
            isDownloaded = true
        )
        assertEquals("ep-123", item.episodeId)
        assertTrue(item.isDownloaded)
    }

    @Test
    fun testPlayAutoAddsToQueue() {
        try {
            state.play("https://example.com/audio1.mp3", "Episode 1")
        } catch (e: Throwable) {
            // Audio player native libraries not available in test
        }

        assertEquals(1, state.queue.size)
        assertEquals("Episode 1", state.queue[0].title)
        assertEquals(0, state.queueIndex)
    }

    @Test
    fun testPlayDoesNotDuplicateInQueue() {
        try {
            state.play("https://example.com/audio1.mp3", "Episode 1")
            state.play("https://example.com/audio1.mp3", "Episode 1")
        } catch (e: Throwable) {}

        assertEquals(1, state.queue.size)
        assertEquals(0, state.queueIndex)
    }

    @Test
    fun testPlayUpdatesQueueIndexForExisting() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")
        state.addToQueue("https://example.com/audio2.mp3", "Episode 2")

        try {
            state.play("https://example.com/audio2.mp3", "Episode 2")
        } catch (e: Throwable) {}

        assertEquals(1, state.queueIndex)
    }

    @Test
    fun testRemoveCurrentlyPlayingPlaysNext() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")
        state.addToQueue("https://example.com/audio2.mp3", "Episode 2")
        state.addToQueue("https://example.com/audio3.mp3", "Episode 3")

        // Simulate playing index 1
        try {
            state.playFromQueue(1)
        } catch (e: Throwable) {}

        // Remove currently playing item
        state.removeFromQueue(1)

        assertEquals(2, state.queue.size)
        assertEquals("Episode 3", state.queue[state.queueIndex].title)
    }

    @Test
    fun testRemoveCurrentlyPlayingStopsWhenEmpty() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")

        try {
            state.playFromQueue(0)
        } catch (e: Throwable) {}

        state.removeFromQueue(0)

        assertEquals(0, state.queue.size)
        assertEquals(-1, state.queueIndex)
    }

    @Test
    fun testRemoveCurrentlyPlayingPlaysFirstWhenNoNext() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")
        state.addToQueue("https://example.com/audio2.mp3", "Episode 2")

        try {
            state.playFromQueue(1)
        } catch (e: Throwable) {}

        // Remove the last item (index 1), should play from index 0
        state.removeFromQueue(1)

        assertEquals(1, state.queue.size)
        assertEquals(0, state.queueIndex)
        assertEquals("Episode 1", state.queue[0].title)
    }

    @Test
    fun testRemoveItemBeforePlayingAdjustsIndex() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")
        state.addToQueue("https://example.com/audio2.mp3", "Episode 2")
        state.addToQueue("https://example.com/audio3.mp3", "Episode 3")

        try {
            state.playFromQueue(2)
        } catch (e: Throwable) {}

        state.removeFromQueue(0)

        assertEquals(2, state.queue.size)
        assertEquals(1, state.queueIndex)
    }

    @Test
    fun testClearQueue() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")
        state.addToQueue("https://example.com/audio2.mp3", "Episode 2")

        try {
            state.playFromQueue(0)
        } catch (e: Throwable) {}

        state.clearQueue()

        assertEquals(0, state.queue.size)
        assertEquals(-1, state.queueIndex)
    }

    @Test
    fun testRemoveSelectedFromQueue() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")
        state.addToQueue("https://example.com/audio2.mp3", "Episode 2")
        state.addToQueue("https://example.com/audio3.mp3", "Episode 3")
        state.addToQueue("https://example.com/audio4.mp3", "Episode 4")

        state.removeSelectedFromQueue(setOf(0, 2))

        assertEquals(2, state.queue.size)
        assertEquals("Episode 2", state.queue[0].title)
        assertEquals("Episode 4", state.queue[1].title)
    }

    @Test
    fun testRemoveSelectedAdjustsPlayingIndex() {
        state.addToQueue("https://example.com/audio1.mp3", "Episode 1")
        state.addToQueue("https://example.com/audio2.mp3", "Episode 2")
        state.addToQueue("https://example.com/audio3.mp3", "Episode 3")

        try {
            state.playFromQueue(2)
        } catch (e: Throwable) {}

        state.removeSelectedFromQueue(setOf(0))

        assertEquals(2, state.queue.size)
        assertEquals(1, state.queueIndex)
    }

    @Test
    fun testAddToQueueWithEpisodeIdAndDownloaded() {
        state.addToQueue(
            url = "https://example.com/audio1.mp3",
            title = "Episode 1",
            episodeId = "ep-123",
            isDownloaded = true
        )

        assertEquals(1, state.queue.size)
        assertEquals("ep-123", state.queue[0].episodeId)
        assertTrue(state.queue[0].isDownloaded)
    }
}
