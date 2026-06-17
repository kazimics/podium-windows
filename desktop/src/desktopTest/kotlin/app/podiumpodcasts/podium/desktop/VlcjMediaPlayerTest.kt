package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.desktop.player.VlcjMediaPlayer
import org.junit.Assume
import kotlin.test.*

class VlcjMediaPlayerTest {

    private var player: VlcjMediaPlayer? = null
    private var vlcAvailable = false

    @BeforeTest
    fun setup() {
        try {
            player = VlcjMediaPlayer()
            vlcAvailable = true
        } catch (e: Exception) {
            vlcAvailable = false
        }
    }

    @AfterTest
    fun teardown() {
        try {
            player?.release()
        } catch (_: Exception) {}
    }

    private fun requireVlc() {
        Assume.assumeTrue("VLC/libvlc not available, skipping test", vlcAvailable)
    }

    @Test
    fun testInitialState() {
        requireVlc()
        val p = player!!
        assertFalse(p.isPlaying)
        assertEquals(0L, p.currentPosition)
        assertEquals(0L, p.duration)
        assertEquals(100, p.volume)
        assertEquals(1.0f, p.playbackSpeed)
    }

    @Test
    fun testSetVolume() {
        requireVlc()
        val p = player!!
        p.setVolume(50)
        assertEquals(50, p.volume)

        p.setVolume(0)
        assertEquals(0, p.volume)

        p.setVolume(100)
        assertEquals(100, p.volume)
    }

    @Test
    fun testSetVolumeClampsValues() {
        requireVlc()
        val p = player!!
        p.setVolume(-10)
        assertEquals(0, p.volume)

        p.setVolume(150)
        assertEquals(100, p.volume)
    }

    @Test
    fun testSetPlaybackSpeed() {
        requireVlc()
        val p = player!!
        p.setPlaybackSpeed(1.5f)
        assertEquals(1.5f, p.playbackSpeed)

        p.setPlaybackSpeed(2.0f)
        assertEquals(2.0f, p.playbackSpeed)
    }

    @Test
    fun testSetPlaybackSpeedClampsValues() {
        requireVlc()
        val p = player!!
        p.setPlaybackSpeed(0.1f)
        assertEquals(0.25f, p.playbackSpeed)

        p.setPlaybackSpeed(5.0f)
        assertEquals(4.0f, p.playbackSpeed)
    }

    @Test
    fun testCallbackOnPlayStateChanged() {
        requireVlc()
        val p = player!!
        var callbackCalled = false
        p.onPlayStateChanged = { state ->
            callbackCalled = true
        }
        assertNotNull(p.onPlayStateChanged)
    }

    @Test
    fun testCallbackOnPositionChanged() {
        requireVlc()
        val p = player!!
        var callbackCalled = false
        p.onPositionChanged = { pos, dur ->
            callbackCalled = true
        }
        assertNotNull(p.onPositionChanged)
    }
}
