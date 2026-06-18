package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration

private const val TAG = "AudioPlayer"

class JfxMediaPlayer {

    private var mediaPlayer: MediaPlayer? = null

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

    fun play(url: String) {
        Logger.i(TAG, "play() url=$url")
        release()

        try {
            val media = Media(url)
            val mp = MediaPlayer(media)

            mp.setOnReady {
                Logger.i(TAG, "Media ready, duration=${mp.totalDuration.toMillis()}ms")
                duration = mp.totalDuration.toMillis().toLong()
                mp.play()
            }

            mp.setOnPlaying {
                Logger.d(TAG, "Playback started")
                isPlaying = true
                onPlayStateChanged?.invoke(true)
                startPositionUpdates(mp)
            }

            mp.setOnPaused {
                Logger.d(TAG, "Playback paused")
                isPlaying = false
                onPlayStateChanged?.invoke(false)
            }

            mp.setOnStopped {
                Logger.d(TAG, "Playback stopped")
                isPlaying = false
                onPlayStateChanged?.invoke(false)
            }

            mp.setOnEndOfMedia {
                Logger.d(TAG, "Playback finished")
                isPlaying = false
                onPlayStateChanged?.invoke(false)
            }

            mp.setOnError {
                val errorMsg = mp.error?.message ?: "Unknown error"
                Logger.e(TAG, "Media error: $errorMsg")
                onError?.invoke(errorMsg)
                isPlaying = false
                onPlayStateChanged?.invoke(false)
            }

            mediaPlayer = mp
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to create media player", e)
            onError?.invoke("Failed to play: ${e.message}")
        }
    }

    fun pause() {
        Logger.d(TAG, "pause()")
        mediaPlayer?.pause()
    }

    fun resume() {
        Logger.d(TAG, "resume()")
        mediaPlayer?.play()
    }

    fun stop() {
        Logger.d(TAG, "stop()")
        mediaPlayer?.stop()
    }

    fun seek(positionMs: Long) {
        mediaPlayer?.seek(Duration.millis(positionMs.toDouble()))
        currentPosition = positionMs
        onPositionChanged?.invoke(currentPosition, duration)
    }

    fun setVolume(vol: Int) {
        val clamped = vol.coerceIn(0, 100)
        mediaPlayer?.volume = clamped / 100.0
        this.volume = clamped
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        mediaPlayer?.rate = clamped.toDouble()
        this.playbackSpeed = clamped
    }

    fun release() {
        mediaPlayer?.dispose()
        mediaPlayer = null
        isPlaying = false
    }

    private fun startPositionUpdates(mp: MediaPlayer) {
        mp.currentTimeProperty().addListener { _, _, newTime ->
            val pos = newTime.toMillis().toLong()
            if (pos >= 0) {
                currentPosition = pos
                onPositionChanged?.invoke(currentPosition, duration)
            }
        }
    }
}
