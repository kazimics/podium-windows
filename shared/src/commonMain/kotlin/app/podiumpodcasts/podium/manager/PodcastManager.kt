package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.api.rss.FetchPodcastClient
import app.podiumpodcasts.podium.api.rss.FetchPodcastClientResult
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import app.podiumpodcasts.podium.utils.RssConverter

sealed class AddPodcastResult {
    data class Duplicate(val duplicate: Podcast) : AddPodcastResult()
    data class Created(val podcast: Podcast) : AddPodcastResult()
}

class PodcastManager(
    private val db: AppDatabase,
    private val fetchPodcastClient: FetchPodcastClient = FetchPodcastClient()
) {
    suspend fun addPodcast(origin: String, seedColor: Int?): AddPodcastResult {
        db.podcasts.getByOrigin(origin)?.let { return AddPodcastResult.Duplicate(it) }

        val response = fetchPodcastClient.fetchNoCache(origin)
        if (response !is FetchPodcastClientResult.Success) throw Exception(response.toString())

        val podcast = RssConverter.toPodcast(response.rssChannel, origin, response.fileSize, seedColor)
        val episodes = response.rssChannel.items.map { RssConverter.toPodcastEpisode(it, podcast) }

        return addPodcast(podcast, episodes, seedColor, false)
    }

    suspend fun addPodcast(podcast: Podcast, episodes: List<PodcastEpisode>, seedColor: Int?, duplicateCheck: Boolean = true): AddPodcastResult {
        if (duplicateCheck) db.podcasts.getByOrigin(podcast.origin)?.let { return AddPodcastResult.Duplicate(it) }

        db.podcasts.insert(podcast)
        episodes.forEach { db.episodes.insert(it) }
        episodes.forEach { db.playStates.initState(it.id) }

        return AddPodcastResult.Created(podcast)
    }
}
