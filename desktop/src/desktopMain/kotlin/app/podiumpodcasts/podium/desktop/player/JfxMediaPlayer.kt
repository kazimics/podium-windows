package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import kotlinx.coroutines.*
import java.io.File

private const val TAG = "AudioPlayer"

class JfxMediaPlayer {

    private var rubberbandPlayer: RubberbandPlayer? = null
    private var currentUrl: String? = null
    private var currentSpeed = 1.0f
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var seekJob: Job? = null

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
        currentUrl = url
        startRubberbandPlayer(url, currentSpeed, 0L)
    }

    private fun startRubberbandPlayer(url: String, speed: Float, startPositionMs: Long) {
        val pp = RubberbandPlayer()
        pp.onPlayStateChanged = { playing ->
            isPlaying = playing
            onPlayStateChanged?.invoke(playing)
        }
        pp.onPositionChanged = { pos ->
            currentPosition = pos
            onPositionChanged?.invoke(currentPosition, duration)
        }
        pp.onError = { msg ->
            Logger.e(TAG, "RubberbandPlayer error: $msg")
            onError?.invoke(msg)
        }
        rubberbandPlayer = pp
        pp.play(url, speed, startPositionMs)
    }

    fun pause() {
        Logger.d(TAG, "pause()")
        rubberbandPlayer?.stop()
        isPlaying = false
        onPlayStateChanged?.invoke(false)
    }

    fun resume() {
        Logger.d(TAG, "resume()")
        if (rubberbandPlayer != null && currentUrl != null) {
            val pp = rubberbandPlayer ?: return
            val pos = pp.currentPosition.coerceAtLeast(currentPosition)
            pp.play(currentUrl!!, currentSpeed, pos)
            pp.onPlayStateChanged = { playing ->
                isPlaying = playing
                onPlayStateChanged?.invoke(playing)
            }
            pp.onPositionChanged = { p ->
                currentPosition = p
                onPositionChanged?.invoke(currentPosition, duration)
            }
            pp.onError = { msg -> onError?.invoke(msg) }
        }
    }

    fun stop() {
        Logger.d(TAG, "stop()")
        seekJob?.cancel()
        rubberbandPlayer?.stop()
        isPlaying = false
        onPlayStateChanged?.invoke(false)
    }

    fun seek(positionMs: Long) {
        currentPosition = positionMs
        onPositionChanged?.invoke(currentPosition, duration)
        if (rubberbandPlayer != null && currentUrl != null) {
            seekJob?.cancel()
            seekJob = scope.launch {
                delay(100)
                rubberbandPlayer?.stop()
                val pp = rubberbandPlayer ?: return@launch
                pp.play(currentUrl!!, currentSpeed, positionMs)
                pp.onPlayStateChanged = { playing ->
                    isPlaying = playing
                    onPlayStateChanged?.invoke(playing)
                }
                pp.onPositionChanged = { pos ->
                    currentPosition = pos
                    onPositionChanged?.invoke(currentPosition, duration)
                }
                pp.onError = { msg -> onError?.invoke(msg) }
            }
        }
    }

    fun setVolume(vol: Int) {
        val clamped = vol.coerceIn(0, 100)
        this.volume = clamped
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        Logger.d(TAG, "setPlaybackSpeed($clamped)")
        currentSpeed = clamped
        this.playbackSpeed = clamped
        rubberbandPlayer?.setPlaybackSpeed(clamped)
    }

    fun release() {
        seekJob?.cancel()
        scope.cancel()
        rubberbandPlayer?.release()
        rubberbandPlayer = null
        isPlaying = false
    }
}
