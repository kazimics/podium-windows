package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

private const val TAG = "MediaPlayerState"

data class QueueItem(
    val url: String,
    val title: String,
    val artworkUrl: String? = null,
    val episodeId: String? = null,
    val isDownloaded: Boolean = false
)

class MediaPlayerState(
    private val player: AudioPlayerEngine = MpvAudioPlayerEngine()
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var sleepTimerJob: Job? = null

    var isPlaying by mutableStateOf(false)
        private set
    var currentPosition by mutableLongStateOf(0L)
        private set
    var duration by mutableLongStateOf(1L)
        private set
    var volume by mutableStateOf(100)
        private set
    private var previousVolumeBeforeMute = 100
    var playbackSpeed by mutableFloatStateOf(1.0f)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var isUserPaused = false

    var currentUrl by mutableStateOf<String?>(null)
        private set
    var currentTitle by mutableStateOf<String?>(null)
        private set
    var currentArtworkUrl by mutableStateOf<String?>(null)
        private set

    var sleepTimerTrigger by mutableStateOf<Long?>(null)
        private set
    var sleepTimerMinutes by mutableStateOf<Int?>(null)
        private set

    val queue = mutableStateListOf<QueueItem>()
    var queueIndex by mutableIntStateOf(-1)
        private set

    init {
        Logger.d(TAG, "MediaPlayerState initialized")
        player.onPlayStateChanged = { playing ->
            Logger.d(TAG, "Play state changed: playing=$playing")
            isPlaying = playing
            isLoading = false
            if (!playing && !isUserPaused) {
                playNext()
            }
        }
        player.onPositionChanged = { pos, dur ->
            currentPosition = pos
            duration = dur
        }
        player.onError = { msg ->
            Logger.e(TAG, "Playback error: $msg")
            error = msg
            isLoading = false
        }
    }

    fun play(url: String, title: String? = null, artworkUrl: String? = null, durationMs: Long = 0L) {
        Logger.i(TAG, "play() title=$title, url=$url, durationMs=$durationMs")
        currentUrl = url
        currentTitle = title
        currentArtworkUrl = artworkUrl
        isLoading = true
        error = null
        isUserPaused = false

        val existingIndex = queue.indexOfFirst { it.url == url }
        if (existingIndex >= 0) {
            queueIndex = existingIndex
        } else {
            queue.add(QueueItem(url, title ?: "Unknown", artworkUrl))
            queueIndex = queue.size - 1
        }

        player.play(url, durationMs = durationMs)
    }

    fun playFromQueue(index: Int) {
        if (index < 0 || index >= queue.size) {
            Logger.w(TAG, "playFromQueue: invalid index=$index, queueSize=${queue.size}")
            return
        }
        queueIndex = index
        val item = queue[index]
        Logger.i(TAG, "playFromQueue: index=$index, title=${item.title}")
        play(item.url, item.title, item.artworkUrl)
    }

    fun addToQueue(
        url: String,
        title: String,
        artworkUrl: String? = null,
        episodeId: String? = null,
        isDownloaded: Boolean = false
    ) {
        Logger.d(TAG, "addToQueue: title=$title")
        queue.add(QueueItem(url, title, artworkUrl, episodeId, isDownloaded))
    }

    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= queue.size) return
        Logger.d(TAG, "removeFromQueue: index=$index")
        val wasPlaying = index == queueIndex
        queue.removeAt(index)
        if (wasPlaying) {
            if (queue.isNotEmpty()) {
                val nextIndex = index.coerceIn(0, queue.size - 1)
                queueIndex = nextIndex
                val item = queue[nextIndex]
                play(item.url, item.title, item.artworkUrl)
            } else {
                queueIndex = -1
                stop()
            }
        } else if (index < queueIndex) {
            queueIndex--
        }
    }

    fun playNext() {
        if (queueIndex + 1 < queue.size) {
            playFromQueue(queueIndex + 1)
        } else {
            Logger.d(TAG, "playNext: no more items in queue")
        }
    }

    fun removeSelectedFromQueue(selectedIndices: Set<Int>) {
        val wasPlayingSelected = queueIndex in selectedIndices
        val sorted = selectedIndices.sortedDescending()
        for (index in sorted) {
            if (index in queue.indices) {
                queue.removeAt(index)
            }
        }
        if (wasPlayingSelected) {
            if (queue.isNotEmpty()) {
                queueIndex = queueIndex.coerceIn(0, queue.size - 1)
                val item = queue[queueIndex]
                play(item.url, item.title, item.artworkUrl)
            } else {
                queueIndex = -1
                stop()
            }
        } else {
            queueIndex = if (queue.isEmpty()) -1
            else queueIndex.coerceIn(0, queue.size - 1)
        }
    }

    fun clearQueue() {
        queue.clear()
        queueIndex = -1
        stop()
    }

    fun playPrevious() {
        if (currentPosition > 3000) {
            Logger.d(TAG, "playPrevious: restarting current track (pos=${currentPosition}ms)")
            seek(0)
        } else if (queueIndex > 0) {
            playFromQueue(queueIndex - 1)
        } else {
            Logger.d(TAG, "playPrevious: at beginning of queue")
        }
    }

    fun pause() {
        Logger.d(TAG, "pause()")
        isUserPaused = true
        player.pause()
    }

    fun resume() {
        Logger.d(TAG, "resume()")
        isUserPaused = false
        player.resume()
    }

    fun togglePlayPause() {
        Logger.d(TAG, "togglePlayPause: isPlaying=$isPlaying")
        if (isPlaying) pause() else resume()
    }

    fun stop() {
        Logger.d(TAG, "stop()")
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

    fun toggleMute() {
        if (volume > 0) {
            previousVolumeBeforeMute = volume
            changeVolume(0)
        } else {
            changeVolume(previousVolumeBeforeMute)
        }
    }

    @Suppress("FunctionName")
    fun changePlaybackSpeed(speed: Float) {
        player.setSpeed(speed)
        playbackSpeed = speed
        Logger.d(TAG, "changePlaybackSpeed: speed=$speed")
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            Logger.d(TAG, "setSleepTimer: cancelled")
            sleepTimerTrigger = null
            sleepTimerMinutes = null
            return
        }
        val triggerTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong())
        sleepTimerTrigger = triggerTime
        sleepTimerMinutes = minutes
        Logger.i(TAG, "setSleepTimer: ${minutes} minutes")

        sleepTimerJob = scope.launch {
            delay(TimeUnit.MINUTES.toMillis(minutes.toLong()))
            withContext(Dispatchers.Main) {
                Logger.i(TAG, "Sleep timer triggered, pausing playback")
                pause()
                sleepTimerTrigger = null
                sleepTimerMinutes = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerTrigger = null
        sleepTimerMinutes = null
    }

    fun getProgress(): Float {
        return if (duration > 0) {
            currentPosition.toFloat() / duration
        } else 0f
    }

    fun release() {
        Logger.d(TAG, "release()")
        sleepTimerJob?.cancel()
        scope.cancel()
        player.release()
    }
}
