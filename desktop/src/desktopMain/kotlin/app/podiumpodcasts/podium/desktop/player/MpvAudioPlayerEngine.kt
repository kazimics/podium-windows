package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import com.sun.jna.Pointer
import kotlin.concurrent.thread

private const val TAG = "MpvAudioPlayerEngine"

class MpvAudioPlayerEngine : AudioPlayerEngine {

    private var mpvHandle: Long = 0L
    private var pollThread: Thread? = null
    private var currentSpeed = 1.0f
    private var currentUrl: String? = null

    @Volatile override var isPlaying = false; private set
    @Volatile override var currentPosition = 0L; private set
    @Volatile override var duration = 0L; private set
    @Volatile override var playbackState: PlaybackState = PlaybackState.IDLE; private set
    @Volatile override var metadata: PlayerMetadata? = null; private set

    override var onPlayStateChanged: ((Boolean) -> Unit)? = null
    override var onPositionChanged: ((Long, Long) -> Unit)? = null
    override var onError: ((String) -> Unit)? = null

    init {
        val loaded = MpvNativeLoader.load()
        if (!loaded) {
            throw IllegalStateException("mpv-1.dll not found. Place it in libs/ directory.")
        }
        mpvHandle = MpvApi.INSTANCE.mpv_create()

        MpvApi.INSTANCE.mpv_set_option_string(mpvHandle, "terminal", "no")
        MpvApi.INSTANCE.mpv_set_option_string(mpvHandle, "audio-only", "yes")
        MpvApi.INSTANCE.mpv_set_option_string(mpvHandle, "video", "no")
        MpvApi.INSTANCE.mpv_set_option_string(mpvHandle, "ao", "wasapi")

        val initResult = MpvApi.INSTANCE.mpv_initialize(mpvHandle)
        if (initResult < 0) {
            Logger.e(TAG, "mpv_initialize failed: $initResult")
            throw IllegalStateException("mpv initialization failed with code $initResult")
        }

        startPolling()
        Logger.i(TAG, "mpv engine initialized")
    }

    private fun startPolling() {
        pollThread = thread(name = "mpv-poll", isDaemon = true) {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep(250)
                    pollProperties()
                } catch (_: InterruptedException) {
                    break
                } catch (e: Exception) {
                    if (!Thread.currentThread().isInterrupted) {
                        Logger.e(TAG, "Poll error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun pollProperties() {
        if (mpvHandle == 0L) return

        val posPtr = MpvApi.INSTANCE.mpv_get_property_string(mpvHandle, "time-pos")
        if (posPtr != Pointer.NULL) {
            try {
                val posStr = posPtr.getString(0)
                val posSec = posStr.toDoubleOrNull()
                if (posSec != null) {
                    val newPos = (posSec * 1000).toLong()
                    if (newPos != currentPosition) {
                        currentPosition = newPos
                        onPositionChanged?.invoke(currentPosition, duration)
                    }
                }
            } finally {
                MpvApi.INSTANCE.mpv_free(posPtr)
            }
        }

        val durPtr = MpvApi.INSTANCE.mpv_get_property_string(mpvHandle, "duration")
        if (durPtr != Pointer.NULL) {
            try {
                val durStr = durPtr.getString(0)
                val durSec = durStr.toDoubleOrNull()
                if (durSec != null) {
                    val newDur = (durSec * 1000).toLong()
                    if (newDur != duration) {
                        duration = newDur
                        metadata = metadata?.copy(durationMs = duration)
                            ?: PlayerMetadata(url = currentUrl ?: "", durationMs = duration)
                        onPositionChanged?.invoke(currentPosition, duration)
                    }
                }
            } finally {
                MpvApi.INSTANCE.mpv_free(durPtr)
            }
        }

        val eofPtr = MpvApi.INSTANCE.mpv_get_property_string(mpvHandle, "eof-reached")
        if (eofPtr != Pointer.NULL) {
            try {
                val eofStr = eofPtr.getString(0)
                if (eofStr == "yes" && isPlaying) {
                    isPlaying = false
                    playbackState = PlaybackState.STOPPED
                    onPlayStateChanged?.invoke(false)
                }
            } finally {
                MpvApi.INSTANCE.mpv_free(eofPtr)
            }
        }

        val pausePtr = MpvApi.INSTANCE.mpv_get_property_string(mpvHandle, "pause")
        if (pausePtr != Pointer.NULL) {
            try {
                val pauseStr = pausePtr.getString(0)
                val paused = pauseStr == "yes"
                if (paused && isPlaying) {
                    isPlaying = false
                    playbackState = PlaybackState.PAUSED
                    onPlayStateChanged?.invoke(false)
                } else if (!paused && !isPlaying && playbackState == PlaybackState.PLAYING) {
                    isPlaying = true
                    onPlayStateChanged?.invoke(true)
                }
            } finally {
                MpvApi.INSTANCE.mpv_free(pausePtr)
            }
        }
    }

    override fun play(url: String, speed: Float, startPositionMs: Long, durationMs: Long) {
        Logger.i(TAG, "play() url=$url, speed=$speed, startPositionMs=$startPositionMs")
        currentUrl = url
        currentSpeed = speed
        playbackState = PlaybackState.LOADING
        metadata = PlayerMetadata(url = url, durationMs = durationMs)

        if (durationMs > 0) {
            duration = durationMs
        }

        MpvApi.command(mpvHandle, "loadfile", url, "replace")
        MpvApi.INSTANCE.mpv_set_property_string(mpvHandle, "speed", speed.toString())

        if (startPositionMs > 0) {
            MpvApi.INSTANCE.mpv_set_property_string(mpvHandle, "start", "${startPositionMs / 1000.0}")
        }

        isPlaying = true
        playbackState = PlaybackState.PLAYING
        onPlayStateChanged?.invoke(true)
    }

    override fun pause() {
        Logger.d(TAG, "pause()")
        MpvApi.INSTANCE.mpv_set_property_string(mpvHandle, "pause", "yes")
        isPlaying = false
        playbackState = PlaybackState.PAUSED
        onPlayStateChanged?.invoke(false)
    }

    override fun resume() {
        Logger.d(TAG, "resume()")
        MpvApi.INSTANCE.mpv_set_property_string(mpvHandle, "pause", "no")
        isPlaying = true
        playbackState = PlaybackState.PLAYING
        onPlayStateChanged?.invoke(true)
    }

    override fun stop() {
        Logger.d(TAG, "stop()")
        MpvApi.command(mpvHandle, "stop")
        isPlaying = false
        playbackState = PlaybackState.STOPPED
    }

    override fun seek(positionMs: Long) {
        Logger.d(TAG, "seek(${positionMs}ms)")
        MpvApi.INSTANCE.mpv_set_property_string(mpvHandle, "time-pos", "${positionMs / 1000.0}")
    }

    override fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        Logger.d(TAG, "setSpeed($clamped)")
        currentSpeed = clamped
        MpvApi.INSTANCE.mpv_set_property_string(mpvHandle, "speed", clamped.toString())
    }

    override fun setVolume(vol: Int) {
        val clamped = vol.coerceIn(0, 100)
        Logger.d(TAG, "setVolume($clamped)")
        MpvApi.INSTANCE.mpv_set_property_string(mpvHandle, "volume", clamped.toString())
    }

    override fun release() {
        Logger.d(TAG, "release()")
        pollThread?.interrupt()
        pollThread = null
        if (mpvHandle != 0L) {
            MpvApi.command(mpvHandle, "quit")
            MpvApi.INSTANCE.mpv_terminate_destroy(mpvHandle)
            mpvHandle = 0L
        }
        isPlaying = false
        playbackState = PlaybackState.IDLE
        metadata = null
    }
}
