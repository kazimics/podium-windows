package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "VlcjMediaPlayer"

class VlcjMediaPlayer {

    private var factory: MediaPlayerFactory? = null
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

    private val positionUpdateExecutor = Executors.newSingleThreadScheduledExecutor()
    private var positionUpdateRunning = false

    private fun findVlcPath(): String? {
        Logger.d(TAG, "Searching for VLC installation...")

        val bundledPath = javaClass.getResource("/vlc")?.toURI()?.let { File(it) }
        if (bundledPath?.exists() == true) {
            Logger.d(TAG, "Found bundled VLC at: ${bundledPath.absolutePath}")
            return bundledPath.absolutePath
        }

        val commonPaths = listOf(
            "C:\\Program Files\\VideoLAN\\VLC",
            "C:\\Program Files (x86)\\VideoLAN\\VLC",
            System.getenv("ProgramFiles") + "\\VideoLAN\\VLC",
            System.getenv("LOCALAPPDATA") + "\\VideoLAN\\VLC"
        )
        for (path in commonPaths) {
            if (File(path, "libvlc.dll").exists()) {
                Logger.d(TAG, "Found VLC at: $path")
                return path
            }
        }

        Logger.w(TAG, "VLC not found in any standard location")
        return null
    }

    private fun ensureInitialized(): MediaPlayer? {
        if (mediaPlayer != null) return mediaPlayer
        return try {
            val vlcPath = findVlcPath()
            val args = mutableListOf("--no-video", "--no-xlib")
            if (vlcPath != null) {
                args.addAll(listOf("--plugin-path", vlcPath))
            }

            Logger.i(TAG, "Creating MediaPlayerFactory with args: $args")
            val f = MediaPlayerFactory(*args.toTypedArray())
            val mp = f.mediaPlayers().newMediaPlayer()
            Logger.i(TAG, "MediaPlayer created successfully")

            mp.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun playing(mediaPlayer: MediaPlayer) {
                    Logger.i(TAG, "Playback state: PLAYING")
                    isPlaying = true
                    onPlayStateChanged?.invoke(true)
                    startPositionUpdates()
                }

                override fun paused(mediaPlayer: MediaPlayer) {
                    Logger.i(TAG, "Playback state: PAUSED")
                    isPlaying = false
                    onPlayStateChanged?.invoke(false)
                    stopPositionUpdates()
                }

                override fun stopped(mediaPlayer: MediaPlayer) {
                    Logger.i(TAG, "Playback state: STOPPED")
                    isPlaying = false
                    onPlayStateChanged?.invoke(false)
                    stopPositionUpdates()
                }

                override fun finished(mediaPlayer: MediaPlayer) {
                    Logger.i(TAG, "Playback state: FINISHED")
                    isPlaying = false
                    onPlayStateChanged?.invoke(false)
                    stopPositionUpdates()
                }

                override fun error(mediaPlayer: MediaPlayer) {
                    Logger.e(TAG, "VLC error event received")
                    onError?.invoke("Playback error occurred")
                    isPlaying = false
                    onPlayStateChanged?.invoke(false)
                    stopPositionUpdates()
                }

                override fun mediaChanged(mediaPlayer: MediaPlayer, mediaRef: uk.co.caprica.vlcj.media.MediaRef) {
                    duration = mediaPlayer.status().length()
                    currentPosition = 0L
                    Logger.d(TAG, "Media changed, duration=${duration}ms")
                    onPositionChanged?.invoke(0L, duration)
                }
            })

            factory = f
            mediaPlayer = mp
            mp
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize VLC", e)
            onError?.invoke("VLC not found: ${e.message}")
            null
        }
    }

    fun play(url: String) {
        Logger.i(TAG, "play() called with url=$url")
        val mp = ensureInitialized()
        if (mp == null) {
            Logger.e(TAG, "Cannot play: MediaPlayer is null (VLC initialization failed)")
            return
        }
        Logger.d(TAG, "Calling media().play()")
        mp.media().play(url)
    }

    fun pause() {
        mediaPlayer?.controls()?.pause()
    }

    fun resume() {
        mediaPlayer?.controls()?.play()
    }

    fun stop() {
        mediaPlayer?.controls()?.stop()
    }

    fun seek(positionMs: Long) {
        mediaPlayer?.controls()?.setTime(positionMs)
        currentPosition = positionMs
        onPositionChanged?.invoke(currentPosition, duration)
    }

    fun setVolume(vol: Int) {
        val clamped = vol.coerceIn(0, 100)
        mediaPlayer?.audio()?.setVolume(clamped)
        this.volume = clamped
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        mediaPlayer?.controls()?.setRate(clamped)
        this.playbackSpeed = clamped
    }

    fun release() {
        stopPositionUpdates()
        mediaPlayer?.release()
        factory?.release()
        mediaPlayer = null
        factory = null
        positionUpdateExecutor.shutdown()
    }

    private fun startPositionUpdates() {
        if (positionUpdateRunning) return
        positionUpdateRunning = true
        positionUpdateExecutor.scheduleAtFixedRate({
            val mp = mediaPlayer
            if (isPlaying && mp?.status()?.isPlaying == true) {
                val pos = mp.status().time()
                val len = mp.status().length()
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
