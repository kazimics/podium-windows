package app.podiumpodcasts.podium.desktop.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MediaPlayerState {

    private val player = VlcjMediaPlayer()

    var isPlaying by mutableStateOf(false)
        private set
    var currentPosition by mutableLongStateOf(0L)
        private set
    var duration by mutableLongStateOf(1L)
        private set
    var volume by mutableStateOf(100)
        private set
    var playbackSpeed by mutableFloatStateOf(1.0f)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var currentUrl by mutableStateOf<String?>(null)
        private set
    var currentTitle by mutableStateOf<String?>(null)
        private set
    var currentArtworkUrl by mutableStateOf<String?>(null)
        private set

    var sleepTimerTrigger by mutableStateOf<Long?>(null)

    init {
        player.onPlayStateChanged = { playing ->
            isPlaying = playing
            isLoading = false
        }
        player.onPositionChanged = { pos, dur ->
            currentPosition = pos
            duration = dur
        }
        player.onError = { msg ->
            error = msg
            isLoading = false
        }
    }

    fun play(url: String, title: String? = null, artworkUrl: String? = null) {
        currentUrl = url
        currentTitle = title
        currentArtworkUrl = artworkUrl
        isLoading = true
        error = null
        player.play(url)
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.resume()
    }

    fun togglePlayPause() {
        if (isPlaying) pause() else resume()
    }

    fun stop() {
        player.stop()
        currentUrl = null
        currentTitle = null
        currentArtworkUrl = null
    }

    fun seek(positionMs: Long) {
        player.seek(positionMs)
    }

    fun seekBack(incrementMs: Long = 10000L) {
        val newPos = (currentPosition - incrementMs).coerceAtLeast(0L)
        seek(newPos)
    }

    fun seekForward(incrementMs: Long = 10000L) {
        val newPos = (currentPosition + incrementMs).coerceAtMost(duration)
        seek(newPos)
    }

    @Suppress("FunctionName")
    fun changeVolume(vol: Int) {
        player.setVolume(vol)
        volume = vol
    }

    @Suppress("FunctionName")
    fun changePlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        playbackSpeed = speed
    }

    fun setSleepTimer(triggerUnixMillis: Long?) {
        sleepTimerTrigger = triggerUnixMillis
    }

    fun getProgress(): Float {
        return if (duration > 0) {
            currentPosition.toFloat() / duration
        } else 0f
    }

    fun release() {
        player.release()
    }
}
