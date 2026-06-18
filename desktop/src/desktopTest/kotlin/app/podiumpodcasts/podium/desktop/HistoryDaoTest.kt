package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class HistoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var testDbFile: File

    @BeforeTest
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
    }

    @AfterTest
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testInsertAndGetAll() = runBlocking {
        database.history.insert("https://example.com/feed.xml", "ep-1")
        database.history.insert("https://example.com/feed.xml", "ep-2")

        val history = database.history.getAllSync()
        assertEquals(2, history.size)
        assertEquals("ep-1", history[1].episodeId)
        assertEquals("ep-2", history[0].episodeId)
    }

    @Test
    fun testDeleteByEpisodeId() = runBlocking {
        database.history.insert("https://example.com/feed.xml", "ep-1")
        database.history.insert("https://example.com/feed.xml", "ep-2")

        database.history.delete("ep-1")

        val history = database.history.getAllSync()
        assertEquals(1, history.size)
        assertEquals("ep-2", history[0].episodeId)
    }

    @Test
    fun testDeleteAll() = runBlocking {
        database.history.insert("https://example.com/feed.xml", "ep-1")
        database.history.insert("https://example.com/feed.xml", "ep-2")
        database.history.insert("https://example.com/feed.xml", "ep-3")

        database.history.deleteAll()

        val history = database.history.getAllSync()
        assertEquals(0, history.size)
    }

    @Test
    fun testGetAllWithEpisode() = runBlocking {
        val podcast = Podcast(
            origin = "https://example.com/feed.xml",
            link = "https://example.com",
            title = "Test Podcast",
            description = "Description",
            author = "Author",
            imageUrl = "https://example.com/image.jpg",
            imageSeedColor = 0,
            languageCode = "en",
            fileSize = 1000L,
            overrideTitle = "",
            skipBeginning = 0,
            skipEnding = 0
        )
        database.podcasts.insert(podcast)

        val episode = PodcastEpisode(
            id = "ep-1", guid = "guid-1", origin = "https://example.com/feed.xml",
            link = "https://example.com/ep1", title = "Episode 1",
            description = "Desc", imageUrl = null, author = "Author",
            pubDate = 1000L, duration = 300, audioUrl = "https://example.com/audio.mp3",
            podcastTitle = "Test Podcast"
        )
        database.episodes.insert(episode)
        database.history.insert("https://example.com/feed.xml", "ep-1")

        val result = database.history.getAllWithEpisode()
        assertEquals(1, result.size)
        assertNotNull(result[0].second)
        assertEquals("Episode 1", result[0].second!!.title)
    }

    @Test
    fun testGetAllWithEpisodeMissingEpisode() = runBlocking {
        database.history.insert("https://example.com/feed.xml", "nonexistent-ep")

        val result = database.history.getAllWithEpisode()
        assertEquals(1, result.size)
        assertNull(result[0].second)
    }

    @Test
    fun testDeleteNonExistentEpisode() = runBlocking {
        database.history.delete("nonexistent")
    }
}
