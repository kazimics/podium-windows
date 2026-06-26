package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.api.apple.ApplePodcastClient
import app.podiumpodcasts.podium.api.rss.FetchPodcastClient
import app.podiumpodcasts.podium.api.rss.FetchPodcastClientResult
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import app.podiumpodcasts.podium.utils.Logger
import app.podiumpodcasts.podium.utils.RssConverter

private const val TAG = "PodcastManager"

sealed class AddPodcastResult {
    data class Duplicate(val duplicate: Podcast) : AddPodcastResult()
    data class Created(val podcast: Podcast) : AddPodcastResult()
}

class PodcastManager(
    private val db: AppDatabase,
    private val fetchPodcastClient: FetchPodcastClient = FetchPodcastClient(),
    private val appleClient: ApplePodcastClient = ApplePodcastClient()
) {
    suspend fun addPodcast(origin: String, seedColor: Int?): AddPodcastResult {
        Logger.i(TAG, "addPodcast: origin=$origin")

        val resolvedOrigin: String
        val displayOrigin: String

        if (origin.startsWith("itunes-lookup:")) {
            val idStr = origin.removePrefix("itunes-lookup:")
            val id = idStr.toLongOrNull()
            if (id == null) {
                throw IllegalArgumentException("Invalid iTunes ID: $idStr")
            }
            Logger.d(TAG, "Resolving iTunes ID: $id")
            val preview = appleClient.lookup.lookupById(id)
                ?: throw Exception("iTunes lookup failed for ID: $id")
            resolvedOrigin = preview.fetchUrl
            displayOrigin = origin
            Logger.d(TAG, "Resolved to RSS feed: $resolvedOrigin")
        } else {
            resolvedOrigin = origin
            displayOrigin = origin
        }

        db.podcasts.getByOrigin(displayOrigin)?.let {
            Logger.d(TAG, "Podcast already exists: ${it.title}")
            return AddPodcastResult.Duplicate(it)
        }

        Logger.d(TAG, "Fetching RSS feed from: $resolvedOrigin")
        val response = fetchPodcastClient.fetchNoCache(resolvedOrigin)
        if (response !is FetchPodcastClientResult.Success) {
            Logger.e(TAG, "Failed to fetch RSS feed: $response")
            throw Exception(response.toString())
        }

        val podcast = RssConverter.toPodcast(response.rssChannel, displayOrigin, response.fileSize, seedColor)
        val episodes = response.rssChannel.items.map { RssConverter.toPodcastEpisode(it, podcast) }
        Logger.i(TAG, "Parsed podcast: ${podcast.title}, ${episodes.size} episodes")

        return addPodcast(podcast, episodes, seedColor, false)
    }

    suspend fun addPodcast(podcast: Podcast, episodes: List<PodcastEpisode>, seedColor: Int?, duplicateCheck: Boolean = true): AddPodcastResult {
        if (duplicateCheck) db.podcasts.getByOrigin(podcast.origin)?.let { return AddPodcastResult.Duplicate(it) }

        Logger.d(TAG, "Inserting podcast into database: ${podcast.title}")
        db.podcasts.insert(podcast)
        episodes.forEach { db.episodes.insert(it) }
        episodes.forEach { db.playStates.initState(it.id) }
        Logger.i(TAG, "Podcast saved: ${podcast.title} (${episodes.size} episodes)")

        return AddPodcastResult.Created(podcast)
    }
}
