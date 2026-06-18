package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import java.io.File
import java.util.jar.JarFile

private const val TAG = "AudioPlayer"

object NativeLibLoader {
    private var loaded = false

    fun load() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            try {
                val nativeDir = File(System.getProperty("user.home"), ".podium/native")
                nativeDir.mkdirs()

                val nativeDlls = listOf("jfxmedia.dll", "gstreamer-lite.dll", "glib-lite.dll", "fxplugins.dll")

                val allExist = nativeDlls.all { File(nativeDir, it).exists() }
                if (!allExist) {
                    Logger.d(TAG, "Extracting JavaFX native libraries...")
                    val jarLocations = listOf(
                        JfxMediaPlayer::class.java.protectionDomain?.codeSource?.location?.toURI()?.path,
                        "libs/javafx-media-21.0.2-win.jar",
                        "../libs/javafx-media-21.0.2-win.jar"
                    )

                    var extracted = false
                    for (jarPath in jarLocations) {
                        if (jarPath != null && jarPath.endsWith(".jar") && File(jarPath).exists()) {
                            Logger.d(TAG, "Found JAR: $jarPath")
                            JarFile(jarPath).use { jar ->
                                for (dllName in nativeDlls) {
                                    val entry = jar.getEntry(dllName) ?: continue
                                    val outFile = File(nativeDir, dllName)
                                    jar.getInputStream(entry).use { input -> outFile.writeBytes(input.readBytes()) }
                                    Logger.d(TAG, "Extracted: $dllName (${outFile.length()} bytes)")
                                    extracted = true
                                }
                            }
                            if (extracted) break
                        }
                    }
                    if (!extracted) Logger.e(TAG, "Could not extract native DLLs")
                }

                for (dllName in nativeDlls) {
                    val dll = File(nativeDir, dllName)
                    if (dll.exists()) {
                        try {
                            Runtime.getRuntime().load(dll.absolutePath)
                            Logger.d(TAG, "Loaded: $dllName")
                        } catch (e: UnsatisfiedLinkError) {
                            Logger.w(TAG, "Cannot load $dllName: ${e.message}")
                        }
                    } else {
                        Logger.e(TAG, "DLL missing: ${dll.absolutePath}")
                    }
                }

                loaded = true
                Logger.i(TAG, "JavaFX native libraries ready")
            } catch (e: Throwable) {
                Logger.e(TAG, "Failed to load JavaFX native libraries", e)
            }
        }
    }
}

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
            NativeLibLoader.load()

            val mediaUrl = if (url.startsWith("http")) url else File(url).toURI().toString()
            Logger.d(TAG, "Media URI: $mediaUrl")

            val media = Media(mediaUrl)
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
        } catch (e: Throwable) {
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
