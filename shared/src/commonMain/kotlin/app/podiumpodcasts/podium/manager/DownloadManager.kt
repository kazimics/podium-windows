package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.data.AppDatabase
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.readBytes
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DownloadManager(
    private val db: AppDatabase,
    private val downloadsDir: File
) {
    private val client = HttpClient()

    suspend fun downloadEpisode(
        episodeId: String,
        audioUrl: String,
        origin: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            db.downloads.add(episodeId)

            val episodeDir = File(downloadsDir, origin.sha256())
            episodeDir.mkdirs()
            val outputFile = File(episodeDir, audioUrl.sha256())

            val response = client.prepareGet(audioUrl).execute { httpResponse ->
                if (!httpResponse.status.isSuccess()) throw Exception("HTTP ${httpResponse.status.value}")
                val bytes = httpResponse.readBytes()
                outputFile.writeBytes(bytes)
                bytes.size.toLong()
            }

            db.downloads.add(episodeId)
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEpisodeDownload(episodeId: String, origin: String, audioUrl: String) {
        val episodeDir = File(downloadsDir, origin.sha256())
        val outputFile = File(episodeDir, audioUrl.sha256())
        if (outputFile.exists()) outputFile.delete()
        db.downloads.delete(episodeId)
    }

    fun getDownloadFile(origin: String, audioUrl: String): File {
        val episodeDir = File(downloadsDir, origin.sha256())
        return File(episodeDir, audioUrl.sha256())
    }
}

fun String.sha256(): String {
    return java.security.MessageDigest.getInstance("SHA-256").digest(this.toByteArray()).joinToString("") { "%02x".format(it) }
}
