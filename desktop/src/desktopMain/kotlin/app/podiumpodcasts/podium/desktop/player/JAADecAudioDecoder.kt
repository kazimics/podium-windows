package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import java.io.InputStream
import java.io.PushbackInputStream

private const val TAG = "JAADecDecoder"

class JAADecAudioDecoder(private val inputStream: InputStream) {
    private var decodedData: ShortArray? = null
    private var decodePosition = 0
    private var totalSamples = 0

    var sampleRate: Int = 44100; private set
    var channels: Int = 2; private set
    var totalDurationMs: Long = 0L; private set

    fun open() {
        Logger.i(TAG, "Opening JAADec decoder...")

        val pushback = PushbackInputStream(inputStream, 16)

        val header = ByteArray(12)
        val read = pushback.read(header)
        if (read < 8) {
            throw IllegalArgumentException("Stream too short")
        }
        pushback.unread(header, 0, read)

        val magic = String(header, 4, 4)
        Logger.i(TAG, "Format: $magic")

        if (magic == "ftyp") {
            decodeMp4(pushback)
        } else {
            throw IllegalArgumentException("Not an MP4/M4A file")
        }
    }

    private fun decodeMp4(pushback: InputStream) {
        Logger.i(TAG, "Decoding MP4 container...")

        try {
            val mp4InputStreamClass = Class.forName("net.sourceforge.jaad.mp4.MP4InputStream")
            val openMethod = mp4InputStreamClass.getMethod("open", InputStream::class.java)
            val mp4InputStream = openMethod.invoke(null, pushback)

            val mp4Class = Class.forName("net.sourceforge.jaad.mp4.MP4Container")
            val mp4Constructor = mp4Class.getDeclaredConstructor(mp4InputStreamClass)
            val mp4Container = mp4Constructor.newInstance(mp4InputStream)

            val getMovieMethod = mp4Class.getMethod("getMovie")
            val movie = getMovieMethod.invoke(mp4Container)

            val getTracksMethod = movie.javaClass.getMethod("getTracks")
            val tracks = getTracksMethod.invoke(movie) as? List<*>

            if (tracks.isNullOrEmpty()) {
                throw IllegalArgumentException("No tracks found in MP4")
            }

            var audioTrack: Any? = null
            for (track in tracks) {
                val getHandlerTypeMethod = track!!.javaClass.getMethod("getHandlerType")
                val handlerType = getHandlerTypeMethod.invoke(track) as? String
                Logger.i(TAG, "Track handler type: $handlerType")
                if (handlerType == "soun") {
                    audioTrack = track
                    break
                }
            }

            if (audioTrack == null) {
                throw IllegalArgumentException("No audio track found")
            }

            val getFrameBoxesMethod = audioTrack.javaClass.getMethod("getFrameBoxes")
            val frameBoxes = getFrameBoxesMethod.invoke(audioTrack) as? List<*>

            if (frameBoxes.isNullOrEmpty()) {
                throw IllegalArgumentException("No frames found in audio track")
            }

            Logger.i(TAG, "Found ${frameBoxes.size} audio frames, decoding...")

            val pcmData = mutableListOf<Short>()
            var sampleCount = 0

            for (frameBox in frameBoxes) {
                try {
                    val getInputStreamMethod = frameBox!!.javaClass.getMethod("getInputStream")
                    val frameStream = getInputStreamMethod.invoke(frameBox) as? InputStream ?: continue

                    val configClass = Class.forName("net.sourceforge.jaad.aac.DecoderConfig")
                    val getConfigMethod = configClass.getMethod("getDecoderConfig", InputStream::class.java)
                    val config = getConfigMethod.invoke(null, frameStream)

                    val decoderClass = Class.forName("net.sourceforge.jaad.aac.Decoder")
                    val decoderConstructor = decoderClass.getDeclaredConstructor(configClass)
                    val decoder = decoderConstructor.newInstance(config)

                    val decodeFrameMethod = decoderClass.getMethod("decodeFrame")
                    val sampleBuffer = decodeFrameMethod.invoke(decoder)

                    if (sampleBuffer != null) {
                        val getBufferMethod = sampleBuffer.javaClass.getMethod("getBuffer")
                        val getBufferLengthMethod = sampleBuffer.javaClass.getMethod("getBufferLength")
                        val buffer = getBufferMethod.invoke(sampleBuffer) as? ShortArray
                        val length = getBufferLengthMethod.invoke(sampleBuffer) as? Int ?: 0

                        if (buffer != null && length > 0) {
                            pcmData.addAll(buffer.take(length).toList())
                            sampleCount += length

                            if (sampleCount <= length) {
                                val getSampleFrequencyMethod = config.javaClass.getMethod("getSampleFrequency")
                                val freq = getSampleFrequencyMethod.invoke(config) as? Int
                                if (freq != null && freq > 0) {
                                    sampleRate = freq
                                }
                                val getChannelConfigurationMethod = config.javaClass.getMethod("getChannelConfiguration")
                                val chanConfig = getChannelConfigurationMethod.invoke(config) as? Int
                                if (chanConfig != null) {
                                    channels = if (chanConfig == 0) 1 else chanConfig.coerceAtMost(2)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to decode frame: ${e.message}")
                }
            }

            if (pcmData.isEmpty()) {
                throw IllegalArgumentException("No audio data decoded")
            }

            decodedData = pcmData.toShortArray()
            totalSamples = decodedData!!.size
            totalDurationMs = (totalSamples * 1000L) / (sampleRate * channels)
            Logger.i(TAG, "Decoded ${totalSamples} samples, sampleRate=$sampleRate, channels=$channels, duration=${totalDurationMs}ms")

        } catch (e: ClassNotFoundException) {
            Logger.e(TAG, "JAADec classes not found: ${e.message}")
            throw IllegalArgumentException("JAADec library not available")
        } catch (e: Exception) {
            Logger.e(TAG, "MP4 decode failed: ${e.message}")
            throw e
        }
    }

    fun readFrame(): ShortArray? {
        if (decodedData == null) return null
        if (decodePosition >= decodedData!!.size) return null

        val frameSize = 4096 * channels
        val end = (decodePosition + frameSize).coerceAtMost(decodedData!!.size)
        val frame = decodedData!!.copyOfRange(decodePosition, end)
        decodePosition = end
        return frame
    }

    fun seekToMs(targetMs: Long) {
        val samplesPerMs = sampleRate / 1000
        val targetSample = (targetMs * samplesPerMs).toInt() * channels
        decodePosition = targetSample.coerceIn(0, (decodedData?.size ?: 0))
    }

    fun close() {
        decodedData = null
        decodePosition = 0
        totalSamples = 0
    }
}
