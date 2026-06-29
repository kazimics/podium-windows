package app.podiumpodcasts.podium.desktop.player

enum class PlaybackState {
    IDLE, LOADING, PLAYING, PAUSED, STOPPED, ERROR
}

data class PlayerMetadata(
    val url: String,
    val title: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long = 0L
)

interface AudioPlayerEngine {
    val isPlaying: Boolean
    val currentPosition: Long
    val duration: Long
    val playbackState: PlaybackState
    val metadata: PlayerMetadata?

    var onPlayStateChanged: ((Boolean) -> Unit)?
    var onPositionChanged: ((Long, Long) -> Unit)?
    var onError: ((String) -> Unit)?

    fun play(url: String, speed: Float = 1.0f, startPositionMs: Long = 0L, durationMs: Long = 0L)
    fun pause()
    fun resume()
    fun stop()
    fun seek(positionMs: Long)
    fun setSpeed(speed: Float)
    fun setVolume(vol: Int)
    fun release()
}
