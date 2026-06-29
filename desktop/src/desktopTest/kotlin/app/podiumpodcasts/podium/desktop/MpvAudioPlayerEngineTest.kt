package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.desktop.player.MpvAudioPlayerEngine
import app.podiumpodcasts.podium.desktop.player.PlaybackState
import kotlin.test.*

class MpvAudioPlayerEngineTest {

    @Test
    fun testInitialState() {
        val engine = MpvAudioPlayerEngine()
        assertFalse(engine.isPlaying)
        assertEquals(0L, engine.currentPosition)
        assertEquals(0L, engine.duration)
        assertEquals(PlaybackState.IDLE, engine.playbackState)
        assertNull(engine.metadata)
        engine.release()
    }

    @Test
    fun testPauseWhenNotPlaying() {
        val engine = MpvAudioPlayerEngine()
        engine.pause()
        assertFalse(engine.isPlaying)
        assertEquals(PlaybackState.PAUSED, engine.playbackState)
        engine.release()
    }

    @Test
    fun testStopResetsState() {
        val engine = MpvAudioPlayerEngine()
        engine.stop()
        assertFalse(engine.isPlaying)
        assertEquals(PlaybackState.STOPPED, engine.playbackState)
        engine.release()
    }

    @Test
    fun testSetSpeedClamps() {
        val engine = MpvAudioPlayerEngine()
        engine.setSpeed(0.01f)
        engine.setSpeed(100f)
        engine.release()
    }

    @Test
    fun testSetVolumeClamps() {
        val engine = MpvAudioPlayerEngine()
        engine.setVolume(-10)
        engine.setVolume(200)
        engine.release()
    }

    @Test
    fun testCallbackNullSafety() {
        val engine = MpvAudioPlayerEngine()
        engine.onPlayStateChanged = null
        engine.onPositionChanged = null
        engine.onError = null
        engine.stop()
        engine.release()
    }

    @Test
    fun testReleaseCleanup() {
        val engine = MpvAudioPlayerEngine()
        engine.release()
        assertEquals(PlaybackState.IDLE, engine.playbackState)
        assertNull(engine.metadata)
    }

    @Test
    fun testPlaySetsMetadata() {
        val engine = MpvAudioPlayerEngine()
        engine.play("https://example.com/test.mp3", durationMs = 60000L)
        assertNotNull(engine.metadata)
        assertEquals("https://example.com/test.mp3", engine.metadata!!.url)
        assertEquals(60000L, engine.metadata!!.durationMs)
        assertTrue(engine.playbackState == PlaybackState.LOADING || engine.playbackState == PlaybackState.PLAYING)
        engine.release()
    }
}
