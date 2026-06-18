package app.podiumpodcasts.podium.desktop.player

import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
        // Check bundled resources first
        val bundledPath = javaClass.getResource("/vlc")?.toURI()?.let { File(it) }
        if (bundledPath?.exists() == true) {
            return bundledPath.absolutePath
        }

        // Check common VLC installation paths
        val commonPaths = listOf(
            "C:\\Program Files\\VideoLAN\\VLC",
            "C:\\Program Files (x86)\\VideoLAN\\VLC",
            System.getenv("ProgramFiles") + "\\VideoLAN\\VLC",
            System.getenv("LOCALAPPDATA") + "\\VideoLAN\\VLC"
        )
        for (path in commonPaths) {
            if (File(path, "libvlc.dll").exists()) {
                return path
            }
        }

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

            val f = MediaPlayerFactory(*args.toTypedArray())
            val mp = f.mediaPlayers().newMediaPlayer()

            mp.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
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

            factory = f
            mediaPlayer = mp
            mp
        } catch (e: Exception) {
            onError?.invoke("VLC not found: ${e.message}")
            null
        }
    }

    fun play(url: String) {
        val mp = ensureInitialized() ?: return
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
