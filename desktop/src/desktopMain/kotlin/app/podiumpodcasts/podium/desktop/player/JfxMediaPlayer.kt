package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import javazoom.jl.player.Player
import java.io.BufferedInputStream
import java.net.URL
import kotlin.concurrent.thread

private const val TAG = "AudioPlayer"

class JfxMediaPlayer {

    private var player: Player? = null
    private var playerThread: Thread? = null
    private var isStopped = false

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
        stop()

        isStopped = false
        playerThread = thread(name = "audio-player", isDaemon = true) {
            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val inputStream = BufferedInputStream(connection.getInputStream())

                Logger.d(TAG, "Connected to audio stream, starting playback")

                val p = Player(inputStream)
                player = p

                isPlaying = true
                onPlayStateChanged?.invoke(true)

                p.play()

                if (!isStopped) {
                    Logger.i(TAG, "Playback completed")
                    isPlaying = false
                    onPlayStateChanged?.invoke(false)
                }
            } catch (e: Exception) {
                if (!isStopped) {
                    Logger.e(TAG, "Playback error: ${e.message}")
                    onError?.invoke("Playback failed: ${e.message}")
                    isPlaying = false
                    onPlayStateChanged?.invoke(false)
                }
            }
        }
    }

    fun pause() {
        Logger.d(TAG, "pause() - JLayer basic player does not support pause")
    }

    fun resume() {
        Logger.d(TAG, "resume() - JLayer basic player does not support resume")
    }

    fun stop() {
        isStopped = true
        try {
            player?.close()
        } catch (_: Exception) {}
        player = null
        playerThread?.interrupt()
        playerThread = null
        isPlaying = false
    }

    fun seek(positionMs: Long) {
        Logger.d(TAG, "seek($positionMs) - JLayer does not support seeking")
    }

    fun setVolume(vol: Int) {
        this.volume = vol.coerceIn(0, 100)
    }

    fun setPlaybackSpeed(speed: Float) {
        this.playbackSpeed = speed.coerceIn(0.25f, 4.0f)
    }

    fun release() {
        stop()
    }
}
