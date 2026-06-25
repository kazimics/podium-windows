package app.podiumpodcasts.podium.desktop.player

import java.io.InputStream

interface AudioDecoder : AutoCloseable {
    val sampleRate: Int
    val channels: Int
    val bitsPerSample: Int
    val totalDurationMs: Long

    fun open(inputStream: InputStream)
    fun readFrame(): ShortArray?
    fun seekToMs(targetMs: Long)
    override fun close()
}