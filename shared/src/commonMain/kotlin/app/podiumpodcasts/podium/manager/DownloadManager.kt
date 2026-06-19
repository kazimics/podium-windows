package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.readBytes
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "DownloadManager"

class DownloadManager(
    private val db: AppDatabase,
    private val downloadsDir: File
) {
    private val client = HttpClient()

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

            // Report 0% before starting
            onProgress?.invoke(0L, 0L)

            client.prepareGet(audioUrl).execute { httpResponse ->
                if (!httpResponse.status.isSuccess()) throw Exception("HTTP ${httpResponse.status.value}")
                val totalBytes = httpResponse.contentLength() ?: 0L
                val bytes = httpResponse.readBytes()
                outputFile.writeBytes(bytes)
                onProgress?.invoke(bytes.size.toLong(), totalBytes)
            }

            Logger.i(TAG, "Download complete: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
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
