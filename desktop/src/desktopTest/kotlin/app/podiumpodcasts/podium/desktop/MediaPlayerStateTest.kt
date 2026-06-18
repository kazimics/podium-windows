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

        state.playFromQueue(1)

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
        // After toggle, should attempt to resume (which is a no-op when stopped)
        assertFalse(state.isPlaying)
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
}
