package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.manager.AddPodcastResult
import app.podiumpodcasts.podium.manager.PodcastManager
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class PodcastManagerTest {

    private lateinit var database: AppDatabase
    private lateinit var manager: PodcastManager
    private lateinit var testDbFile: File
    private val fakeOrigin = "https://fake-podcast.example.com/feed.xml"

    @BeforeTest
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
        manager = PodcastManager(database, fetchPodcastClient = FakeFetchPodcastClient())
    }

    @AfterTest
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testAddPodcastCreatesNewPodcast() = runBlocking {
        val result = manager.addPodcast(fakeOrigin, null)

        assertTrue(result is AddPodcastResult.Created)
        val podcast = (result as AddPodcastResult.Created).podcast
        assertNotNull(podcast.title)
        assertTrue(podcast.title.isNotEmpty())
    }

    @Test
    fun testAddPodcastDuplicateReturnsDuplicate() = runBlocking {
        val firstResult = manager.addPodcast(fakeOrigin, null)
        assertTrue(firstResult is AddPodcastResult.Created)

        val secondResult = manager.addPodcast(fakeOrigin, null)
        assertTrue(secondResult is AddPodcastResult.Duplicate)
    }

    @Test
    fun testAddPodcastSavesToDatabase() = runBlocking {
        manager.addPodcast(fakeOrigin, null)

        val podcasts = database.podcasts.getAllSync()
        assertEquals(1, podcasts.size)
        assertEquals(fakeOrigin, podcasts[0].origin)
    }

    @Test
    fun testAddPodcastSavesEpisodes() = runBlocking {
        manager.addPodcast(fakeOrigin, null)

        val episodes = database.episodes.getAllByOrigin(fakeOrigin)
        assertEquals(2, episodes.size)
        val titles = episodes.map { it.title }.toSet()
        assertTrue(titles.containsAll(setOf("Episode 1", "Episode 2")))
    }

    @Test
    fun testAddInvalidUrlStillWorks() = runBlocking {
        val result = manager.addPodcast("https://invalid-url.example.com/feed.xml", null)
        assertTrue(result is AddPodcastResult.Created)
    }
}
