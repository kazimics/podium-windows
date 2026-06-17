package app.podiumpodcasts.podium.data

import app.cash.sqldelight.db.SqlDriver
import app.podiumpodcasts.podium.data.model.*
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppDatabase(driver: SqlDriver) {
    val database = PodiumDatabase(driver)
    val podcasts = PodcastDao(database)
    val episodes = EpisodeDao(database)
    val playStates = PlayStateDao(database)
    val downloads = DownloadDao(database)
    val history = HistoryDao(database)
    val subscriptions = SubscriptionDao(database)
    val lists = ListDao(database)
    val listItems = ListItemDao(database)
    val syncActions = SyncActionDao(database)
}

class PodcastDao(private val db: PodiumDatabase) {
    fun getAll(): Flow<List<Podcast>> {
        val flow = MutableStateFlow<List<Podcast>>(emptyList())
        kotlinx.coroutines.CoroutineScope(Dispatchers.Default).kotlinx.coroutines.launch {
            flow.value = getAllSync()
        }
        return flow.asStateFlow()
    }

    suspend fun getAllSync(): List<Podcast> = withContext(Dispatchers.Default) {
        db.podcastQueries.getAll().executeAsList().map {
            Podcast(
                origin = it.origin, link = it.link, title = it.title,
                description = it.description, author = it.author, imageUrl = it.imageUrl,
                imageSeedColor = it.imageSeedColor.toInt(), languageCode = it.languageCode,
                fileSize = it.fileSize, overrideTitle = it.overrideTitle,
                skipBeginning = it.skipBeginning.toInt(), skipEnding = it.skipEnding.toInt()
            )
        }
    }

    suspend fun getByOrigin(origin: String): Podcast? = withContext(Dispatchers.Default) {
        db.podcastQueries.getByOrigin(origin).executeAsOneOrNull()?.let {
            Podcast(
                origin = it.origin, link = it.link, title = it.title,
                description = it.description, author = it.author, imageUrl = it.imageUrl,
                imageSeedColor = it.imageSeedColor.toInt(), languageCode = it.languageCode,
                fileSize = it.fileSize, overrideTitle = it.overrideTitle,
                skipBeginning = it.skipBeginning.toInt(), skipEnding = it.skipEnding.toInt()
            )
        }
    }

    suspend fun insert(podcast: Podcast) = withContext(Dispatchers.Default) {
        db.podcastQueries.insert(
            podcast.origin, podcast.link, podcast.title, podcast.description,
            podcast.author, podcast.imageUrl, podcast.imageSeedColor.toLong(),
            podcast.languageCode, podcast.fileSize, podcast.overrideTitle,
            podcast.skipBeginning.toLong(), podcast.skipEnding.toLong()
        )
    }

    suspend fun delete(origin: String) = withContext(Dispatchers.Default) {
        db.podcastQueries.delete(origin)
    }
}

class EpisodeDao(private val db: PodiumDatabase) {
    suspend fun getAllByOrigin(origin: String): List<PodcastEpisode> = withContext(Dispatchers.Default) {
        db.podcastEpisodeQueries.getAllByOrigin(origin).executeAsList().map {
            PodcastEpisode(
                id = it.id, guid = it.guid, origin = it.origin, link = it.link,
                title = it.title, description = it.description, imageUrl = it.imageUrl,
                author = it.author, pubDate = it.pubDate, duration = it.duration.toInt(),
                audioUrl = it.audioUrl, podcastTitle = it.podcastTitle,
                imageSeedColor = it.imageSeedColor.toInt(), isNew = it.new == 1L
            )
        }
    }

    suspend fun getById(id: String): PodcastEpisode? = withContext(Dispatchers.Default) {
        db.podcastEpisodeQueries.getById(id).executeAsOneOrNull()?.let {
            PodcastEpisode(
                id = it.id, guid = it.guid, origin = it.origin, link = it.link,
                title = it.title, description = it.description, imageUrl = it.imageUrl,
                author = it.author, pubDate = it.pubDate, duration = it.duration.toInt(),
                audioUrl = it.audioUrl, podcastTitle = it.podcastTitle,
                imageSeedColor = it.imageSeedColor.toInt(), isNew = it.new == 1L
            )
        }
    }

    suspend fun getEpisodeIds(origin: String): List<String> = withContext(Dispatchers.Default) {
        db.podcastEpisodeQueries.getEpisodeIds(origin).executeAsList()
    }

    suspend fun insert(episode: PodcastEpisode) = withContext(Dispatchers.Default) {
        db.podcastEpisodeQueries.insert(
            episode.id, episode.guid, episode.origin, episode.link,
            episode.title, episode.description, episode.imageUrl,
            episode.author, episode.pubDate, episode.duration.toLong(),
            episode.audioUrl, episode.podcastTitle, episode.imageSeedColor.toLong(),
            if (episode.isNew) 1L else 0L
        )
    }

    suspend fun markAsNew(id: String) = withContext(Dispatchers.Default) {
        db.podcastEpisodeQueries.markAsNew(id)
    }

    suspend fun markAsNotNew(id: String) = withContext(Dispatchers.Default) {
        db.podcastEpisodeQueries.markAsNotNew(id)
    }
}

class PlayStateDao(private val db: PodiumDatabase) {
    suspend fun initState(episodeId: String) = withContext(Dispatchers.Default) {
        db.podcastEpisodePlayStateQueries.initState(episodeId)
    }

    suspend fun saveState(episodeId: String, state: Int) = withContext(Dispatchers.Default) {
        db.podcastEpisodePlayStateQueries.saveState(state.toLong(), episodeId)
    }

    suspend fun savePlayed(episodeId: String, played: Boolean) = withContext(Dispatchers.Default) {
        db.podcastEpisodePlayStateQueries.savePlayed(if (played) 1L else 0L, episodeId)
    }
}

class DownloadDao(private val db: PodiumDatabase) {
    suspend fun add(episodeId: String) = withContext(Dispatchers.Default) {
        db.podcastEpisodeDownloadQueries.add(episodeId)
    }

    suspend fun delete(episodeId: String) = withContext(Dispatchers.Default) {
        db.podcastEpisodeDownloadQueries.delete(episodeId)
    }

    suspend fun getAllNotDownloaded(): List<String> = withContext(Dispatchers.Default) {
        db.podcastEpisodeDownloadQueries.getAllNotDownloaded().executeAsList()
    }
}

class HistoryDao(private val db: PodiumDatabase) {
    suspend fun getAllSync(): List<PodcastHistory> = withContext(Dispatchers.Default) {
        db.podcastHistoryQueries.getAll().executeAsList().map {
            PodcastHistory(id = it.id.toInt(), origin = it.origin, episodeId = it.episodeId, timestamp = it.timestamp)
        }
    }

    suspend fun insert(origin: String, episodeId: String) = withContext(Dispatchers.Default) {
        db.podcastHistoryQueries.insert(origin, episodeId, kotlin.time.TimeSource.Monotonic.markNow().toEpochMilliseconds())
    }

    suspend fun delete(episodeId: String) = withContext(Dispatchers.Default) {
        db.podcastHistoryQueries.delete(episodeId)
    }
}

class SubscriptionDao(private val db: PodiumDatabase) {
    suspend fun getAllSync(): List<PodcastSubscription> = withContext(Dispatchers.Default) {
        db.podcastSubscriptionQueries.getAll().executeAsList().map {
            PodcastSubscription(
                origin = it.origin, enableNotifications = it.enableNotifications == 1L,
                enableAutoDownload = it.enableAutoDownload == 1L, lastUpdate = it.lastUpdate,
                newEpisodes = it.newEpisodes.toInt(), cacheETag = it.cacheETag,
                cacheLastModified = it.cacheLastModified, cacheContentLength = it.cacheContentLength
            )
        }
    }

    suspend fun getByOriginSync(origin: String): PodcastSubscription? = withContext(Dispatchers.Default) {
        db.podcastSubscriptionQueries.getByOrigin(origin).executeAsOneOrNull()?.let {
            PodcastSubscription(
                origin = it.origin, enableNotifications = it.enableNotifications == 1L,
                enableAutoDownload = it.enableAutoDownload == 1L, lastUpdate = it.lastUpdate,
                newEpisodes = it.newEpisodes.toInt(), cacheETag = it.cacheETag,
                cacheLastModified = it.cacheLastModified, cacheContentLength = it.cacheContentLength
            )
        }
    }

    suspend fun insert(origin: String, enableNotifications: Boolean, enableAutoDownload: Boolean) = withContext(Dispatchers.Default) {
        db.podcastSubscriptionQueries.insert(
            origin, if (enableNotifications) 1L else 0L, if (enableAutoDownload) 1L else 0L,
            0, 0, "", "", ""
        )
    }

    suspend fun updateCache(origin: String, eTag: String, lastModified: String, contentLength: String) = withContext(Dispatchers.Default) {
        db.podcastSubscriptionQueries.updateCache(eTag, lastModified, contentLength, origin)
    }

    suspend fun updateLastUpdate(origin: String, lastUpdate: Long) = withContext(Dispatchers.Default) {
        db.podcastSubscriptionQueries.updateLastUpdate(lastUpdate, origin)
    }

    suspend fun delete(origin: String) = withContext(Dispatchers.Default) {
        db.podcastSubscriptionQueries.delete(origin)
    }
}

class ListDao(private val db: PodiumDatabase) {
    suspend fun createFavorites() = withContext(Dispatchers.Default) {
        db.listQueries.createFavorites(kotlin.time.TimeSource.Monotonic.markNow().toEpochMilliseconds())
    }

    suspend fun createHearLater() = withContext(Dispatchers.Default) {
        db.listQueries.createHearLater(kotlin.time.TimeSource.Monotonic.markNow().toEpochMilliseconds())
    }

    suspend fun getAllSync(): List<app.podiumpodcasts.podium.data.model.ListModel> = withContext(Dispatchers.Default) {
        db.listQueries.getAll().executeAsList().map {
            app.podiumpodcasts.podium.data.model.ListModel(
                id = it.id.toInt(), name = it.name, description = it.description,
                itemCount = it.itemCount.toInt(), imageUrls = it.imageUrls,
                createdAt = it.createdAt, isSystemList = it.isSystemList == 1L
            )
        }
    }

    suspend fun insert(name: String, description: String, imageUrls: String?) = withContext(Dispatchers.Default) {
        db.listQueries.insert(name, description, imageUrls, kotlin.time.TimeSource.Monotonic.markNow().toEpochMilliseconds())
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.Default) {
        db.listQueries.delete(id.toLong())
    }
}

class ListItemDao(private val db: PodiumDatabase) {
    suspend fun getItemsByListIdSync(listId: Int): List<ListItem> = withContext(Dispatchers.Default) {
        db.listItemQueries.getByListId(listId.toLong()).executeAsList().map {
            ListItem(id = it.id.toInt(), listId = it.listId.toInt(), contentId = it.contentId, isPodcast = it.isPodcast == 1L, position = it.position.toInt())
        }
    }

    suspend fun insert(listId: Int, contentId: String, isPodcast: Boolean, position: Int) = withContext(Dispatchers.Default) {
        db.listItemQueries.insert(listId.toLong(), contentId, if (isPodcast) 1L else 0L, position.toLong())
    }

    suspend fun removeByListIdAndContentId(listId: Int, contentId: String) = withContext(Dispatchers.Default) {
        db.listItemQueries.removeByListIdAndContentId(listId.toLong(), contentId)
    }
}

class SyncActionDao(private val db: PodiumDatabase) {
    suspend fun getAll(): List<SyncAction> = withContext(Dispatchers.Default) {
        db.syncActionQueries.getAll().executeAsList().map {
            SyncAction(id = it.id, actionType = it.actionType, origin = it.origin, audioUrl = it.audioUrl, position = it.position?.toInt(), total = it.total?.toInt(), timestamp = it.timestamp)
        }
    }

    suspend fun addAction(id: String, actionType: String, origin: String, audioUrl: String?, position: Int?, total: Int?) = withContext(Dispatchers.Default) {
        db.syncActionQueries.addAction(id, actionType, origin, audioUrl, position?.toLong(), total?.toLong(), kotlin.time.TimeSource.Monotonic.markNow().toEpochMilliseconds())
    }

    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        db.syncActionQueries.delete(id)
    }

    suspend fun deleteAll() = withContext(Dispatchers.Default) {
        db.syncActionQueries.deleteAll()
    }
}
