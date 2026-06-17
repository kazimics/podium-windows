package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.data.AppDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClient
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

@Serializable
data class DeviceUpdate(
    val podcast: String,
    val episode: String,
    val action: String,
    val timestamp: Long,
    val position: Int? = null,
    val total: Int? = null
)

sealed class SyncResult {
    data class Success(val actionCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
    data object NoActions : SyncResult()
}

class SyncManager(
    private val db: AppDatabase,
    private val client: HttpClient = HttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sync(baseUrl: String, username: String, password: String, deviceId: String): SyncResult {
        return try {
            val actions = db.syncActions.getAll()
            if (actions.isEmpty()) return SyncResult.NoActions

            val deviceUpdates = actions.map { action ->
                DeviceUpdate(
                    podcast = action.origin,
                    episode = action.audioUrl ?: "",
                    action = action.actionType.lowercase(),
                    timestamp = action.timestamp / 1000,
                    position = action.position,
                    total = action.total
                )
            }

            val response = client.post("$baseUrl/api/2/subscriptions/$username/$deviceId.json") {
                basicAuth(username, password)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(deviceUpdates))
            }

            if (response.status == HttpStatusCode.OK) {
                actions.forEach { db.syncActions.delete(it.id) }
                SyncResult.Success(actions.size)
            } else {
                SyncResult.Error("HTTP ${response.status.value}: ${response.bodyAsText()}")
            }
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }
}
