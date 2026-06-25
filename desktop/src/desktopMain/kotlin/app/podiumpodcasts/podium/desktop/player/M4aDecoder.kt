package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

private const val TAG = "M4aDecoder"
private const val FFMPEG_DIR_NAME = "ffmpeg"
private const val FFMPEG_EXE_NAME = "ffmpeg.exe"
private const val FFMPEG_DOWNLOAD_URL =
    "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"
private const val PCM_QUEUE_CAPACITY = 200

class M4aDecoder : AudioDecoder {
    override var sampleRate: Int = 44100; private set
    override var channels: Int = 2; private set
    override val bitsPerSample: Int = 16
    override var totalDurationMs: Long = 0L; private set

    private var process: Process? = null
    private val pcmQueue = LinkedBlockingQueue<ShortArray>(PCM_QUEUE_CAPACITY)
    private var readerThread: Thread? = null
    private var inputThread: Thread? = null
    private var stderrThread: Thread? = null
    private var isClosed = false
    private var readFrameCount = 0L

    fun openUrl(url: String, startPositionMs: Long = 0L) {
        Logger.i(TAG, "Starting streaming FFmpeg decode from URL, startPositionMs=$startPositionMs")
        val ffmpegPath = ensureFFmpeg()
        Logger.i(TAG, "FFmpeg found at: $ffmpegPath")

        val args = mutableListOf(
            ffmpegPath,
        )
        if (startPositionMs > 0) {
            val posSec = startPositionMs / 1000.0
            args.addAll(listOf("-ss", "%.3f".format(posSec)))
        }
        args.addAll(listOf(
            "-i", url,
            "-f", "s16le",
            "-acodec", "pcm_s16le",
            "-ar", "44100",
            "-ac", "2",
            "-loglevel", "info",
            "pipe:1"
        ))
        val pb = ProcessBuilder(args)
        pb.redirectErrorStream(false)
        process = pb.start()

        stderrThread = Thread({
            try {
                val reader = process!!.errorStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line!!
                    Logger.d(TAG, "FFmpeg stderr: $l")
                    if (l.contains("Duration:") && !l.contains("Duration: N/A")) {
                        val match = Regex("Duration:\\s*(\\d{2}):(\\d{2}):(\\d{2}\\.\\d{2})").find(l)
                        if (match != null) {
                            val h = match.groupValues[1].toLong()
                            val m = match.groupValues[2].toLong()
                            val s = match.groupValues[3].toDouble()
                            totalDurationMs = ((h * 3600 + m * 60) * 1000 + (s * 1000).toLong())
                            Logger.i(TAG, "Duration from FFmpeg: ${totalDurationMs}ms")
                        }
                    }
                }
            } catch (_: Exception) {}
        }, "ffmpeg-stderr").apply { isDaemon = true; start() }

        readerThread = Thread({
            try {
                val stdout = process!!.inputStream
                val buf = ByteArray(4096 * 2 * 2)
                while (!isClosed && !Thread.currentThread().isInterrupted) {
                    val read = stdout.read(buf)
                    if (read == -1) break
                    if (read > 0) {
                        val shorts = ShortArray(read / 2)
                        for (i in shorts.indices) {
                            shorts[i] = ((buf[i * 2 + 1].toInt() shl 8) or (buf[i * 2].toInt() and 0xFF)).toShort()
                        }
                        pcmQueue.put(shorts)
                    }
                }
                Logger.i(TAG, "FFmpeg output ended, ${pcmQueue.size} frames remaining in queue")
            } catch (e: Exception) {
                if (!isClosed) Logger.e(TAG, "PCM read error: ${e.message}")
            }
        }, "ffmpeg-pcm-reader").apply { isDaemon = true; start() }

        sampleRate = 44100
        channels = 2
        Logger.i(TAG, "Streaming decoder ready, sampleRate=$sampleRate, channels=$channels")
    }

    override fun open(inputStream: InputStream) {
        Logger.i(TAG, "Starting streaming FFmpeg decode...")
        val ffmpegPath = ensureFFmpeg()
        Logger.i(TAG, "FFmpeg found at: $ffmpegPath")

        val pb = ProcessBuilder(
            ffmpegPath,
            "-i", "pipe:0",
            "-f", "s16le",
            "-acodec", "pcm_s16le",
            "-ar", "44100",
            "-ac", "2",
            "-loglevel", "info",
            "pipe:1"
        )
        pb.redirectErrorStream(false)
        process = pb.start()

        inputThread = Thread({
            try {
                process!!.outputStream.buffered(65536).use { ffmpegIn ->
                    inputStream.use { src ->
                        src.copyTo(ffmpegIn, bufferSize = 65536)
                    }
                }
            } catch (e: Exception) {
                if (!isClosed) Logger.e(TAG, "Input pipe error: ${e.message}")
            }
        }, "ffmpeg-input").apply { isDaemon = true; start() }

        val stderrThread = Thread({
            try {
                val reader = process!!.errorStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line!!
                    Logger.d(TAG, "FFmpeg stderr: $l")
                    if (l.contains("Duration:")) {
                        val match = Regex("Duration:\\s*(\\d{2}):(\\d{2}):(\\d{2}\\.\\d{2})").find(l)
                        if (match != null) {
                            val h = match.groupValues[1].toLong()
                            val m = match.groupValues[2].toLong()
                            val s = match.groupValues[3].toDouble()
                            totalDurationMs = ((h * 3600 + m * 60) * 1000 + (s * 1000).toLong())
                            Logger.i(TAG, "Duration from FFmpeg: ${totalDurationMs}ms")
                        }
                    }
                }
            } catch (_: Exception) {}
        }, "ffmpeg-stderr").apply { isDaemon = true; start() }

        readerThread = Thread({
            try {
                val stdout = process!!.inputStream
                val buf = ByteArray(4096 * 2 * 2)
                while (!isClosed && !Thread.currentThread().isInterrupted) {
                    val read = stdout.read(buf)
                    if (read == -1) break
                    if (read > 0) {
                        val shorts = ShortArray(read / 2)
                        for (i in shorts.indices) {
                            shorts[i] = ((buf[i * 2 + 1].toInt() shl 8) or (buf[i * 2].toInt() and 0xFF)).toShort()
                        }
                        pcmQueue.put(shorts)
                    }
                }
                Logger.i(TAG, "FFmpeg output ended, ${pcmQueue.size} frames remaining in queue")
            } catch (e: Exception) {
                if (!isClosed) Logger.e(TAG, "PCM read error: ${e.message}")
            }
        }, "ffmpeg-pcm-reader").apply { isDaemon = true; start() }

        sampleRate = 44100
        channels = 2
        Logger.i(TAG, "Streaming decoder ready, sampleRate=$sampleRate, channels=$channels")
    }

    override fun readFrame(): ShortArray? {
        if (isClosed) return null
        val frame = pcmQueue.poll(2, TimeUnit.SECONDS) ?: return null
        readFrameCount++
        if (readFrameCount == 1L) {
            Logger.i(TAG, "First PCM frame received, playback starting")
        }
        return frame
    }

    override fun seekToMs(targetMs: Long) {
        readFrameCount = 0
    }

    override fun close() {
        isClosed = true
        readerThread?.interrupt()
        inputThread?.interrupt()
        process?.destroyForcibly()
        pcmQueue.clear()
        Logger.i(TAG, "Decoder closed")
    }

    private fun ensureFFmpeg(): String {
        val bundledPath = File(System.getProperty("user.home"), ".podium/$FFMPEG_DIR_NAME/$FFMPEG_EXE_NAME")
        if (bundledPath.exists()) return bundledPath.absolutePath

        val pathEnv = System.getenv("PATH") ?: ""
        for (dir in pathEnv.split(File.pathSeparator)) {
            val f = File(dir, FFMPEG_EXE_NAME)
            if (f.exists()) return f.absolutePath
        }

        val winGetLinks = File(System.getenv("LOCALAPPDATA") ?: "", "Microsoft/WinGet/Links/$FFMPEG_EXE_NAME")
        if (winGetLinks.exists()) return winGetLinks.absolutePath

        Logger.i(TAG, "FFmpeg not found, downloading...")
        downloadFFmpeg(bundledPath)
        return bundledPath.absolutePath
    }

    private fun downloadFFmpeg(targetExe: File) {
        val ffmpegDir = targetExe.parentFile
        ffmpegDir.mkdirs()

        val zipFile = File(ffmpegDir, "ffmpeg.zip")
        try {
            Logger.i(TAG, "Downloading FFmpeg from $FFMPEG_DOWNLOAD_URL ...")
            val conn = URL(FFMPEG_DOWNLOAD_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 30000
            conn.readTimeout = 120000
            conn.instanceFollowRedirects = true

            val responseCode = conn.responseCode
            if (responseCode !in 200..399) {
                throw IOException("HTTP $responseCode downloading FFmpeg")
            }

            val inputStream = conn.inputStream
            zipFile.outputStream().use { out ->
                inputStream.copyTo(out, bufferSize = 65536)
            }
            inputStream.close()
            conn.disconnect()
            Logger.i(TAG, "Downloaded ${zipFile.length()} bytes")

            Logger.i(TAG, "Extracting FFmpeg...")
            ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.endsWith("/$FFMPEG_EXE_NAME") || name == FFMPEG_EXE_NAME) {
                        Logger.i(TAG, "Extracting: $name")
                        Files.copy(zis, targetExe.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        break
                    }
                    entry = zis.nextEntry
                }
            }

            if (!targetExe.exists()) {
                throw IOException("FFmpeg.exe not found in downloaded archive")
            }

            Logger.i(TAG, "FFmpeg extracted to ${targetExe.absolutePath}")
        } finally {
            zipFile.delete()
        }
    }
}
