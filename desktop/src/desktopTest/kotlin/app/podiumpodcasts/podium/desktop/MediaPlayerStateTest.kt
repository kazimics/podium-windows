package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.desktop.player.MediaPlayerState
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
}
