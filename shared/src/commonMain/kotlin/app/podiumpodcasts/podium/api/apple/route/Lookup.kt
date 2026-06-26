package app.podiumpodcasts.podium.api.apple.route

import app.podiumpodcasts.podium.api.apple.ApplePodcastClient
import app.podiumpodcasts.podium.api.model.PodcastPreviewModel
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import app.podiumpodcasts.podium.api.apple.json

@Serializable
data class LookupResponse(
    val resultCount: Long,
    val results: List<LookupResult>,
)

@Serializable
data class LookupResult(
    val feedUrl: String? = null,
    val trackViewUrl: String,
    val trackName: String,
    val artistName: String,
    val artworkUrl600: String,
    val releaseDate: String? = null,
    val country: String,
)

class Lookup(
    val client: ApplePodcastClient
) {

    suspend fun lookupById(id: Long): PodcastPreviewModel? {
        val body = client.httpClient.get("https://itunes.apple.com/lookup?id=$id&media=podcast").body<String>()
        val response = json.decodeFromString<LookupResponse>(body)
        val result = response.results.firstOrNull() ?: return null
        val feedUrl = result.feedUrl ?: return null

        return PodcastPreviewModel(
            fetchUrl = feedUrl,
            link = result.trackViewUrl,
            title = result.trackName,
            description = result.releaseDate ?: "",
            author = result.artistName,
            imageUrl = result.artworkUrl600,
            languageCode = result.country.lowercase()
        )
    }

    suspend fun batchLookupFeedUrls(ids: List<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        val idParam = ids.joinToString(",")
        val body = client.httpClient.get("https://itunes.apple.com/lookup?id=$idParam&media=podcast").body<String>()
        val response = json.decodeFromString<LookupResponse>(body)
        return response.results.mapNotNull { result ->
            val feedUrl = result.feedUrl ?: return@mapNotNull null
            val id = result.trackViewUrl.substringAfterLast("/id").substringBefore("/").toLongOrNull()
                ?: return@mapNotNull null
            id to feedUrl
        }.toMap()
    }

}
