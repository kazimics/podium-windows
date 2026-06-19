package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

private const val TAG = "DownloadManager"

class DownloadManager(
    private val db: AppDatabase,
    private val downloadsDir: File
) {
    suspend fun downloadEpisode(
        episodeId: String,
        audioUrl: String,
        origin: String,
        episodeTitle: String = episodeId,
        podcastTitle: String = "Unknown",
        onProgress: ((Long, Long) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val podcastDir = File(downloadsDir, sanitizeFileName(podcastTitle))
            podcastDir.mkdirs()
            val ext = audioUrl.substringAfterLast('.', "mp3").substringBefore('?')
            val outputFile = File(podcastDir, "${sanitizeFileName(episodeTitle)}.$ext")
            Logger.i(TAG, "Downloading to: ${outputFile.absolutePath}")

            onProgress?.invoke(0L, 0L)

            val url = URL(audioUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            val totalBytes = connection.contentLength.toLong()
            val inputStream = connection.getInputStream()
            val buffer = ByteArray(8192)
            var bytesRead = 0L
            val outputStream = outputFile.outputStream()
            try {
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    outputStream.write(buffer, 0, read)
                    bytesRead += read
                    onProgress?.invoke(bytesRead, totalBytes)
                }
            } finally {
                outputStream.close()
                inputStream.close()
            }

            Logger.i(TAG, "Download complete: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            db.downloads.insert(episodeId, origin, outputFile.absolutePath, podcastTitle)
            Result.success(outputFile)
        } catch (e: Exception) {
            Logger.e(TAG, "Download failed: $audioUrl", e)
            Result.failure(e)
        }
    }

    fun getDownloadFile(origin: String, audioUrl: String, episodeTitle: String = "", podcastTitle: String = ""): File {
        if (episodeTitle.isNotEmpty() && podcastTitle.isNotEmpty()) {
            val podcastDir = File(downloadsDir, sanitizeFileName(podcastTitle))
            val ext = audioUrl.substringAfterLast('.', "mp3").substringBefore('?')
            return File(podcastDir, "${sanitizeFileName(episodeTitle)}.$ext")
        }
        val episodeDir = File(downloadsDir, origin.sha256())
        val ext = audioUrl.substringAfterLast('.', "mp3").substringBefore('?')
        return File(episodeDir, "${audioUrl.sha256()}.$ext")
    }

    fun sanitizeFileName(name: String): String {
        val illegal = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        var result = name
        for (c in illegal) {
            result = result.replace(c, '_')
        }
        return result.trim()
    }
}

fun String.sha256(): String {
    return java.security.MessageDigest.getInstance("SHA-256").digest(this.toByteArray()).joinToString("") { "%02x".format(it) }
}
