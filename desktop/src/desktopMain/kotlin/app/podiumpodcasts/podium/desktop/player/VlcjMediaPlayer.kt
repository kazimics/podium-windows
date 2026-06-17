package app.podiumpodcasts.podium.desktop.player

import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VlcjMediaPlayer {

    private val factory: MediaPlayerFactory = MediaPlayerFactory(
        "--no-video",
        "--no-xlib"
    )

    private val mediaPlayer: MediaPlayer = factory.mediaPlayers().newMediaPlayer()

    var isPlaying = false
        private set
    var currentPosition = 0L
        private set
    var duration = 0L
        private set
    var volume = 100
        private set
    var playbackSpeed = 1.0f
        private set

    var onPlayStateChanged: ((Boolean) -> Unit)? = null
    var onPositionChanged: ((Long, Long) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val positionUpdateExecutor = Executors.newSingleThreadScheduledExecutor()
    private var positionUpdateRunning = false

    init {
        mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                isPlaying = true
                onPlayStateChanged?.invoke(true)
                startPositionUpdates()
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                isPlaying = false
                onPlayStateChanged?.invoke(false)
                stopPositionUpdates()
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                isPlaying = false
                onPlayStateChanged?.invoke(false)
                stopPositionUpdates()
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                isPlaying = false
                onPlayStateChanged?.invoke(false)
                stopPositionUpdates()
            }

            override fun error(mediaPlayer: MediaPlayer) {
                onError?.invoke("Playback error occurred")
                isPlaying = false
                onPlayStateChanged?.invoke(false)
                stopPositionUpdates()
            }

            override fun mediaChanged(mediaPlayer: MediaPlayer, mediaRef: uk.co.caprica.vlcj.media.MediaRef) {
                duration = mediaPlayer.status().length()
                currentPosition = 0L
                onPositionChanged?.invoke(0L, duration)
            }
        })
    }

    fun play(url: String) {
        mediaPlayer.media().play(url)
    }

    fun pause() {
        mediaPlayer.controls().pause()
    }

    fun resume() {
        mediaPlayer.controls().play()
    }

    fun stop() {
        mediaPlayer.controls().stop()
    }

    fun seek(positionMs: Long) {
        mediaPlayer.controls().setTime(positionMs)
        currentPosition = positionMs
        onPositionChanged?.invoke(currentPosition, duration)
    }

    fun setVolume(vol: Int) {
        val clamped = vol.coerceIn(0, 100)
        mediaPlayer.audio().setVolume(clamped)
        this.volume = clamped
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        mediaPlayer.controls().setRate(clamped)
        this.playbackSpeed = clamped
    }

    fun release() {
        stopPositionUpdates()
        mediaPlayer.release()
        factory.release()
        positionUpdateExecutor.shutdown()
    }

    private fun startPositionUpdates() {
        if (positionUpdateRunning) return
        positionUpdateRunning = true
        positionUpdateExecutor.scheduleAtFixedRate({
            if (isPlaying && mediaPlayer.status().isPlaying) {
                val pos = mediaPlayer.status().time()
                val len = mediaPlayer.status().length()
                if (pos >= 0) currentPosition = pos
                if (len > 0) duration = len
                onPositionChanged?.invoke(currentPosition, duration)
            }
        }, 250, 250, TimeUnit.MILLISECONDS)
    }

    private fun stopPositionUpdates() {
        positionUpdateRunning = false
    }
}
