package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.api.rss.FetchPodcastClient
import app.podiumpodcasts.podium.api.rss.FetchPodcastClientResult
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.utils.RssConverter

class SubscriptionManager(
    private val db: AppDatabase,
    private val fetchPodcastClient: FetchPodcastClient = FetchPodcastClient()
) {
    suspend fun subscribe(origin: String) {
        db.subscriptions.insert(origin, false, false)
    }

    suspend fun unsubscribe(origin: String) {
        db.subscriptions.delete(origin)
    }

    suspend fun isSubscribed(origin: String): Boolean {
        return db.subscriptions.getByOriginSync(origin) != null
    }

    suspend fun updatePodcast(origin: String, seedColor: Int?): UpdatePodcastResult {
        val subscription = db.subscriptions.getByOriginSync(origin) ?: return UpdatePodcastResult.NotSubscribed

        val response = fetchPodcastClient.fetch(origin, subscription.cacheLastModified, subscription.cacheETag, subscription.cacheContentLength)
        when (response) {
            is FetchPodcastClientResult.Unchanged -> return UpdatePodcastResult.Unchanged(response.reason)
            is FetchPodcastClientResult.Failure -> return UpdatePodcastResult.Error(response.e)
            is FetchPodcastClientResult.Success -> {
                val podcast = RssConverter.toPodcast(response.rssChannel, origin, response.fileSize, seedColor)
                val episodes = response.rssChannel.items.map { RssConverter.toPodcastEpisode(it, podcast) }

                db.subscriptions.updateCache(origin, response.eTag, response.lastModified, response.contentLength)

                val existingIds = db.episodes.getEpisodeIds(origin)
                val newEpisodes = episodes.filter { it.id !in existingIds }
                newEpisodes.forEach { db.episodes.insert(it) }

                return UpdatePodcastResult.Updated(podcast, newEpisodes.size)
            }
        }
    }
}

sealed class UpdatePodcastResult {
    data class Updated(val podcast: app.podiumpodcasts.podium.data.model.Podcast, val newEpisodesCount: Int) : UpdatePodcastResult()
    data class Unchanged(val reason: String) : UpdatePodcastResult()
    data class Error(val exception: Exception) : UpdatePodcastResult()
    data object NotSubscribed : UpdatePodcastResult()
}
