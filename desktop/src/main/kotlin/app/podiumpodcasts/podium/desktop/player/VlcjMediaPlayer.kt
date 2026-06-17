package app.podiumpodcasts.podium.desktop.player

import uk.co.caprica.vlcj.player.base.MediaApi
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.io.File
import javax.swing.SwingUtilities

class VlcjMediaPlayer {
    private var mediaPlayerComponent: EmbeddedMediaPlayerComponent? = null
    private var mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer? = null

    private var _isPlaying = false
    private var _currentPosition = 0L
    private var _duration = 0L

    val isPlaying: Boolean get() = _isPlaying
    val currentPosition: Long get() = _currentPosition
    val duration: Long get() = _duration

    fun initialize() {
        SwingUtilities.invokeAndWait {
            mediaPlayerComponent = EmbeddedMediaPlayerComponent()
            mediaPlayer = mediaPlayerComponent?.mediaPlayer()
        }
    }

    fun play(url: String) {
        mediaPlayer?.media()?.play(url)
        _isPlaying = true
    }

    fun pause() {
        mediaPlayer?.controls()?.pause()
        _isPlaying = false
    }

    fun resume() {
        mediaPlayer?.controls()?.play()
        _isPlaying = true
    }

    fun stop() {
        mediaPlayer?.controls()?.stop()
        _isPlaying = false
    }

    fun seek(positionMs: Long) {
        mediaPlayer?.controls()?.seek(positionMs)
        _currentPosition = positionMs
    }

    fun setVolume(volume: Int) {
        mediaPlayer?.audio()?.volume = volume
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaPlayer?.controls()?.setSpeed(speed)
    }

    fun updateProgress() {
        mediaPlayer?.let { player ->
            _currentPosition = player.status().time()
            _duration = player.media().info()?.duration() ?: 0L
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayerComponent?.release()
        mediaPlayer = null
        mediaPlayerComponent = null
    }
}
