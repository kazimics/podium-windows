package app.podiumpodcasts.podium.data

import app.podiumpodcasts.podium.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class AppDatabase private constructor(private val connection: Connection) {

    companion object {
        fun build(databaseFile: File): AppDatabase {
            databaseFile.parentFile?.mkdirs()
            val connection = DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}")
            val db = AppDatabase(connection)
            db.createTables()
            return db
        }
    }

    private fun createTables() {
        connection.createStatement().use { stmt ->
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS podcast (
                    origin TEXT NOT NULL PRIMARY KEY,
                    link TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    author TEXT NOT NULL,
                    imageUrl TEXT NOT NULL,
                    imageSeedColor INTEGER NOT NULL DEFAULT 0,
                    languageCode TEXT NOT NULL,
                    fileSize INTEGER NOT NULL DEFAULT 0,
                    overrideTitle TEXT NOT NULL DEFAULT '',
                    skipBeginning INTEGER NOT NULL DEFAULT 0,
                    skipEnding INTEGER NOT NULL DEFAULT 0
                )
            """)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS podcastEpisode (
                    id TEXT NOT NULL PRIMARY KEY,
                    guid TEXT NOT NULL,
                    origin TEXT NOT NULL,
                    link TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    imageUrl TEXT,
                    author TEXT NOT NULL,
                    pubDate INTEGER NOT NULL,
                    duration INTEGER NOT NULL,
                    audioUrl TEXT NOT NULL,
                    podcastTitle TEXT NOT NULL,
                    imageSeedColor INTEGER NOT NULL DEFAULT 0,
                    new INTEGER NOT NULL DEFAULT 0
                )
            """)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS podcastEpisodePlayState (
                    episodeId TEXT NOT NULL PRIMARY KEY,
                    state INTEGER NOT NULL DEFAULT 0,
                    played INTEGER NOT NULL DEFAULT 0,
                    lastUpdate INTEGER NOT NULL DEFAULT 0
                )
            """)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS podcastHistory (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    origin TEXT NOT NULL,
                    episodeId TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS podcastSubscription (
                    origin TEXT NOT NULL PRIMARY KEY,
                    enableNotifications INTEGER NOT NULL DEFAULT 0,
                    enableAutoDownload INTEGER NOT NULL DEFAULT 0,
                    lastUpdate INTEGER NOT NULL DEFAULT 0,
                    newEpisodes INTEGER NOT NULL DEFAULT 0,
                    cacheETag TEXT NOT NULL DEFAULT '',
                    cacheLastModified TEXT NOT NULL DEFAULT '',
                    cacheContentLength TEXT NOT NULL DEFAULT ''
                )
            """)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS syncAction (
                    id TEXT NOT NULL PRIMARY KEY,
                    actionType TEXT NOT NULL,
                    origin TEXT NOT NULL,
                    audioUrl TEXT,
                    position INTEGER,
                    total INTEGER,
                    timestamp INTEGER NOT NULL
                )
             """)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS podcastDownload (
                    episodeId TEXT NOT NULL PRIMARY KEY,
                    origin TEXT NOT NULL,
                    filePath TEXT NOT NULL,
                    podcastTitle TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """)
        }
    }

    val podcasts = PodcastDao(connection)
    val episodes = EpisodeDao(connection)
    val playStates = PlayStateDao(connection)
    val history = HistoryDao(connection)
    val subscriptions = SubscriptionDao(connection)
    val syncActions = SyncActionDao(connection)
    val downloads = DownloadDao(connection)

    fun close() { connection.close() }
}

class PodcastDao(private val conn: Connection) {
    suspend fun getAllSync(): List<Podcast> = withContext(Dispatchers.IO) {
        val rs = conn.createStatement().executeQuery("SELECT * FROM podcast ORDER BY title ASC")
        val list = mutableListOf<Podcast>()
        while (rs.next()) {
            list.add(Podcast(
                origin = rs.getString("origin"), link = rs.getString("link"), title = rs.getString("title"),
                description = rs.getString("description"), author = rs.getString("author"),
                imageUrl = rs.getString("imageUrl"), imageSeedColor = rs.getInt("imageSeedColor"),
                languageCode = rs.getString("languageCode"), fileSize = rs.getLong("fileSize"),
                overrideTitle = rs.getString("overrideTitle"), skipBeginning = rs.getInt("skipBeginning"),
                skipEnding = rs.getInt("skipEnding")
            ))
        }
        list
    }

    suspend fun getAllOrigins(): Set<String> = withContext(Dispatchers.IO) {
        val rs = conn.createStatement().executeQuery("SELECT origin FROM podcast")
        val origins = mutableSetOf<String>()
        while (rs.next()) origins.add(rs.getString("origin"))
        origins
    }

    suspend fun getByOrigin(origin: String): Podcast? = withContext(Dispatchers.IO) {
        val ps = conn.prepareStatement("SELECT * FROM podcast WHERE origin = ?")
        ps.setString(1, origin)
        val rs = ps.executeQuery()
        if (rs.next()) Podcast(
            origin = rs.getString("origin"), link = rs.getString("link"), title = rs.getString("title"),
            description = rs.getString("description"), author = rs.getString("author"),
            imageUrl = rs.getString("imageUrl"), imageSeedColor = rs.getInt("imageSeedColor"),
            languageCode = rs.getString("languageCode"), fileSize = rs.getLong("fileSize"),
            overrideTitle = rs.getString("overrideTitle"), skipBeginning = rs.getInt("skipBeginning"),
            skipEnding = rs.getInt("skipEnding")
        ) else null
    }

    suspend fun insert(podcast: Podcast) = withContext(Dispatchers.IO) {
        val ps = conn.prepareStatement(
            "INSERT OR IGNORE INTO podcast (origin, link, title, description, author, imageUrl, imageSeedColor, languageCode, fileSize, overrideTitle, skipBeginning, skipEnding) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )
        ps.setString(1, podcast.origin); ps.setString(2, podcast.link); ps.setString(3, podcast.title)
        ps.setString(4, podcast.description); ps.setString(5, podcast.author); ps.setString(6, podcast.imageUrl)
        ps.setInt(7, podcast.imageSeedColor); ps.setString(8, podcast.languageCode); ps.setLong(9, podcast.fileSize)
        ps.setString(10, podcast.overrideTitle); ps.setInt(11, podcast.skipBeginning); ps.setInt(12, podcast.skipEnding)
        ps.executeUpdate()
    }

    suspend fun delete(origin: String) = withContext(Dispatchers.IO) {
        conn.prepareStatement("DELETE FROM podcast WHERE origin = ?").apply {
            setString(1, origin); executeUpdate()
        }
    }
}

class EpisodeDao(private val conn: Connection) {
    suspend fun getAllByOrigin(origin: String): List<PodcastEpisode> = withContext(Dispatchers.IO) {
        val ps = conn.prepareStatement("SELECT * FROM podcastEpisode WHERE origin = ? ORDER BY pubDate DESC")
        ps.setString(1, origin)
        val rs = ps.executeQuery()
        val list = mutableListOf<PodcastEpisode>()
        while (rs.next()) list.add(readEpisode(rs))
        list
    }

    suspend fun getById(id: String): PodcastEpisode? = withContext(Dispatchers.IO) {
        val ps = conn.prepareStatement("SELECT * FROM podcastEpisode WHERE id = ?")
        ps.setString(1, id)
        val rs = ps.executeQuery()
        if (rs.next()) readEpisode(rs) else null
    }

    suspend fun getEpisodeIds(origin: String): List<String> = withContext(Dispatchers.IO) {
        val ps = conn.prepareStatement("SELECT id FROM podcastEpisode WHERE origin = ?")
        ps.setString(1, origin)
        val rs = ps.executeQuery()
        val list = mutableListOf<String>()
        while (rs.next()) list.add(rs.getString("id"))
        list
    }

    suspend fun deleteByOrigin(origin: String) = withContext(Dispatchers.IO) {
        conn.prepareStatement("DELETE FROM podcastEpisode WHERE origin = ?").apply {
            setString(1, origin); executeUpdate()
        }
    }

    suspend fun insert(episode: PodcastEpisode) = withContext(Dispatchers.IO) {
        val ps = conn.prepareStatement(
            "INSERT OR IGNORE INTO podcastEpisode (id, guid, origin, link, title, description, imageUrl, author, pubDate, duration, audioUrl, podcastTitle, imageSeedColor, new) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )
        ps.setString(1, episode.id); ps.setString(2, episode.guid); ps.setString(3, episode.origin)
        ps.setString(4, episode.link); ps.setString(5, episode.title); ps.setString(6, episode.description)
        ps.setString(7, episode.imageUrl); ps.setString(8, episode.author); ps.setLong(9, episode.pubDate)
        ps.setInt(10, episode.duration); ps.setString(11, episode.audioUrl); ps.setString(12, episode.podcastTitle)
        ps.setInt(13, episode.imageSeedColor); ps.setInt(14, if (episode.isNew) 1 else 0)
        ps.executeUpdate()
    }

    suspend fun markAsNew(id: String) = withContext(Dispatchers.IO) {
        conn.prepareStatement("UPDATE podcastEpisode SET new = 1 WHERE id = ?").apply {
            setString(1, id); executeUpdate()
        }
    }

    suspend fun markAsNotNew(id: String) = withContext(Dispatchers.IO) {
        conn.prepareStatement("UPDATE podcastEpisode SET new = 0 WHERE id = ?").apply {
            setString(1, id); executeUpdate()
        }
    }

    private fun readEpisode(rs: java.sql.ResultSet) = PodcastEpisode(
        id = rs.getString("id"), guid = rs.getString("guid"), origin = rs.getString("origin"),
        link = rs.getString("link"), title = rs.getString("title"), description = rs.getString("description"),
        imageUrl = rs.getString("imageUrl"), author = rs.getString("author"), pubDate = rs.getLong("pubDate"),
        duration = rs.getInt("duration"), audioUrl = rs.getString("audioUrl"),
        podcastTitle = rs.getString("podcastTitle"), imageSeedColor = rs.getInt("imageSeedColor"),
        isNew = rs.getInt("new") == 1
    )
}

class PlayStateDao(private val conn: Connection) {
    suspend fun initState(episodeId: String) = withContext(Dispatchers.IO) {
        conn.prepareStatement("INSERT OR IGNORE INTO podcastEpisodePlayState (episodeId, state, played, lastUpdate) VALUES (?, 0, 0, 0)").apply {
            setString(1, episodeId); executeUpdate()
        }
    }

    suspend fun saveState(episodeId: String, state: Int) = withContext(Dispatchers.IO) {
        conn.prepareStatement("UPDATE podcastEpisodePlayState SET state = ? WHERE episodeId = ?").apply {
            setInt(1, state); setString(2, episodeId); executeUpdate()
        }
    }

    suspend fun savePlayed(episodeId: String, played: Boolean) = withContext(Dispatchers.IO) {
        conn.prepareStatement("UPDATE podcastEpisodePlayState SET played = ? WHERE episodeId = ?").apply {
            setInt(1, if (played) 1 else 0); setString(2, episodeId); executeUpdate()
        }
    }
}

class HistoryDao(private val conn: Connection) {
    suspend fun getAllSync(): List<PodcastHistory> = withContext(Dispatchers.IO) {
        val rs = conn.createStatement().executeQuery("SELECT * FROM podcastHistory ORDER BY timestamp DESC")
        val list = mutableListOf<PodcastHistory>()
        while (rs.next()) list.add(PodcastHistory(
            id = rs.getInt("id"), origin = rs.getString("origin"),
            episodeId = rs.getString("episodeId"), timestamp = rs.getLong("timestamp")
        ))
        list
    }

    suspend fun getAllWithEpisode(): List<Pair<PodcastHistory, PodcastEpisode?>> = withContext(Dispatchers.IO) {
        val rs = conn.createStatement().executeQuery(
            """SELECT h.id, h.origin, h.episodeId, h.timestamp,
               e.title as epTitle, e.audioUrl as epAudioUrl, e.imageUrl as epImageUrl,
               e.podcastTitle as epPodcastTitle, e.duration as epDuration,
               p.imageUrl as podcastImageUrl
               FROM podcastHistory h
               LEFT JOIN podcastEpisode e ON h.episodeId = e.id
               LEFT JOIN podcast p ON h.origin = p.origin
               ORDER BY h.timestamp DESC"""
        )
        val list = mutableListOf<Pair<PodcastHistory, PodcastEpisode?>>()
        while (rs.next()) {
            val history = PodcastHistory(
                id = rs.getInt("id"), origin = rs.getString("origin"),
                episodeId = rs.getString("episodeId"), timestamp = rs.getLong("timestamp")
            )
            val epTitle = rs.getString("epTitle")
            val episode = if (epTitle != null) PodcastEpisode(
                id = history.episodeId, guid = "", origin = history.origin,
                link = "", title = epTitle, description = "",
                imageUrl = rs.getString("epImageUrl") ?: rs.getString("podcastImageUrl"), author = "",
                pubDate = 0, duration = rs.getInt("epDuration"),
                audioUrl = rs.getString("epAudioUrl") ?: "",
                podcastTitle = rs.getString("epPodcastTitle") ?: ""
            ) else null
            list.add(history to episode)
        }
        list
    }

    suspend fun insert(origin: String, episodeId: String) = withContext(Dispatchers.IO) {
        val ts = System.currentTimeMillis()
        conn.prepareStatement("INSERT INTO podcastHistory (origin, episodeId, timestamp) VALUES (?, ?, ?)").apply {
            setString(1, origin); setString(2, episodeId); setLong(3, ts); executeUpdate()
        }
    }

    suspend fun delete(episodeId: String) = withContext(Dispatchers.IO) {
        conn.prepareStatement("DELETE FROM podcastHistory WHERE episodeId = ?").apply {
            setString(1, episodeId); executeUpdate()
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        conn.createStatement().executeUpdate("DELETE FROM podcastHistory")
    }
}

class SubscriptionDao(private val conn: Connection) {
    suspend fun getAllSync(): List<PodcastSubscription> = withContext(Dispatchers.IO) {
        val rs = conn.createStatement().executeQuery("SELECT * FROM podcastSubscription ORDER BY origin ASC")
        val list = mutableListOf<PodcastSubscription>()
        while (rs.next()) list.add(PodcastSubscription(
            origin = rs.getString("origin"), enableNotifications = rs.getInt("enableNotifications") == 1,
            enableAutoDownload = rs.getInt("enableAutoDownload") == 1, lastUpdate = rs.getLong("lastUpdate"),
            newEpisodes = rs.getInt("newEpisodes"), cacheETag = rs.getString("cacheETag"),
            cacheLastModified = rs.getString("cacheLastModified"), cacheContentLength = rs.getString("cacheContentLength")
        ))
        list
    }

    suspend fun getByOriginSync(origin: String): PodcastSubscription? = withContext(Dispatchers.IO) {
        val ps = conn.prepareStatement("SELECT * FROM podcastSubscription WHERE origin = ?")
        ps.setString(1, origin)
        val rs = ps.executeQuery()
        if (rs.next()) PodcastSubscription(
            origin = rs.getString("origin"), enableNotifications = rs.getInt("enableNotifications") == 1,
            enableAutoDownload = rs.getInt("enableAutoDownload") == 1, lastUpdate = rs.getLong("lastUpdate"),
            newEpisodes = rs.getInt("newEpisodes"), cacheETag = rs.getString("cacheETag"),
            cacheLastModified = rs.getString("cacheLastModified"), cacheContentLength = rs.getString("cacheContentLength")
        ) else null
    }

    suspend fun insert(origin: String, enableNotifications: Boolean, enableAutoDownload: Boolean) = withContext(Dispatchers.IO) {
        conn.prepareStatement("INSERT OR IGNORE INTO podcastSubscription (origin, enableNotifications, enableAutoDownload, lastUpdate, newEpisodes, cacheETag, cacheLastModified, cacheContentLength) VALUES (?, ?, ?, 0, 0, '', '', '')").apply {
            setString(1, origin); setInt(2, if (enableNotifications) 1 else 0); setInt(3, if (enableAutoDownload) 1 else 0)
            executeUpdate()
        }
    }

    suspend fun updateCache(origin: String, eTag: String, lastModified: String, contentLength: String) = withContext(Dispatchers.IO) {
        conn.prepareStatement("UPDATE podcastSubscription SET cacheETag = ?, cacheLastModified = ?, cacheContentLength = ? WHERE origin = ?").apply {
            setString(1, eTag); setString(2, lastModified); setString(3, contentLength); setString(4, origin)
            executeUpdate()
        }
    }

    suspend fun updateLastUpdate(origin: String, lastUpdate: Long) = withContext(Dispatchers.IO) {
        conn.prepareStatement("UPDATE podcastSubscription SET lastUpdate = ? WHERE origin = ?").apply {
            setLong(1, lastUpdate); setString(2, origin); executeUpdate()
        }
    }

    suspend fun delete(origin: String) = withContext(Dispatchers.IO) {
        conn.prepareStatement("DELETE FROM podcastSubscription WHERE origin = ?").apply {
            setString(1, origin); executeUpdate()
        }
    }
}

class SyncActionDao(private val conn: Connection) {
    suspend fun getAll(): List<SyncAction> = withContext(Dispatchers.IO) {
        val rs = conn.createStatement().executeQuery("SELECT * FROM syncAction ORDER BY timestamp ASC")
        val list = mutableListOf<SyncAction>()
        while (rs.next()) list.add(SyncAction(
            id = rs.getString("id"), actionType = rs.getString("actionType"), origin = rs.getString("origin"),
            audioUrl = rs.getString("audioUrl"), position = rs.getObject("position") as? Int,
            total = rs.getObject("total") as? Int, timestamp = rs.getLong("timestamp")
        ))
        list
    }

    suspend fun addAction(id: String, actionType: String, origin: String, audioUrl: String?, position: Int?, total: Int?) = withContext(Dispatchers.IO) {
        val ts = System.currentTimeMillis()
        conn.prepareStatement("INSERT OR IGNORE INTO syncAction (id, actionType, origin, audioUrl, position, total, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)").apply {
            setString(1, id); setString(2, actionType); setString(3, origin); setString(4, audioUrl)
            if (position != null) setInt(5, position) else setNull(5, java.sql.Types.INTEGER)
            if (total != null) setInt(6, total) else setNull(6, java.sql.Types.INTEGER)
            setLong(7, ts); executeUpdate()
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        conn.prepareStatement("DELETE FROM syncAction WHERE id = ?").apply {
            setString(1, id); executeUpdate()
        }
    }
}

data class PodcastDownload(
    val episodeId: String,
    val origin: String,
    val filePath: String,
    val podcastTitle: String,
    val timestamp: Long
)

class DownloadDao(private val conn: Connection) {
    suspend fun insert(episodeId: String, origin: String, filePath: String, podcastTitle: String) = withContext(Dispatchers.IO) {
        val ts = System.currentTimeMillis()
        conn.prepareStatement("INSERT OR REPLACE INTO podcastDownload (episodeId, origin, filePath, podcastTitle, timestamp) VALUES (?, ?, ?, ?, ?)").apply {
            setString(1, episodeId); setString(2, origin); setString(3, filePath)
            setString(4, podcastTitle); setLong(5, ts); executeUpdate()
        }
    }

    suspend fun getByEpisodeId(episodeId: String): PodcastDownload? = withContext(Dispatchers.IO) {
        val ps = conn.prepareStatement("SELECT * FROM podcastDownload WHERE episodeId = ?")
        ps.setString(1, episodeId)
        val rs = ps.executeQuery()
        if (rs.next()) PodcastDownload(
            episodeId = rs.getString("episodeId"),
            origin = rs.getString("origin"),
            filePath = rs.getString("filePath"),
            podcastTitle = rs.getString("podcastTitle"),
            timestamp = rs.getLong("timestamp")
        ) else null
    }

    suspend fun getAllDownloadedIds(): Set<String> = withContext(Dispatchers.IO) {
        val rs = conn.createStatement().executeQuery("SELECT episodeId FROM podcastDownload")
        val ids = mutableSetOf<String>()
        while (rs.next()) { ids.add(rs.getString("episodeId")) }
        ids
    }

    suspend fun getAllValidDownloadedIds(): Set<String> = withContext(Dispatchers.IO) {
        val rs = conn.createStatement().executeQuery("SELECT episodeId, filePath FROM podcastDownload")
        val ids = mutableSetOf<String>()
        val toDelete = mutableListOf<String>()
        while (rs.next()) {
            val episodeId = rs.getString("episodeId")
            val filePath = rs.getString("filePath")
            if (java.io.File(filePath).exists()) {
                ids.add(episodeId)
            } else {
                toDelete.add(episodeId)
            }
        }
        for (id in toDelete) {
            delete(id)
        }
        ids
    }

    suspend fun delete(episodeId: String) = withContext(Dispatchers.IO) {
        conn.prepareStatement("DELETE FROM podcastDownload WHERE episodeId = ?").apply {
            setString(1, episodeId); executeUpdate()
        }
    }
}
