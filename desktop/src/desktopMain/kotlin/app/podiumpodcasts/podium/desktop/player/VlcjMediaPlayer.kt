package app.podiumpodcasts.podium.desktop.player

class VlcjMediaPlayer {
    var isPlaying = false
        private set
    var currentPosition = 0L
        private set
    var duration = 0L
        private set

    fun play(url: String) { isPlaying = true }
    fun pause() { isPlaying = false }
    fun resume() { isPlaying = true }
    fun stop() { isPlaying = false }
    fun seek(positionMs: Long) { currentPosition = positionMs }
    fun setVolume(volume: Int) {}
    fun setPlaybackSpeed(speed: Float) {}
    fun release() { isPlaying = false }
}
