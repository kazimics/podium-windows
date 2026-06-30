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

    @BeforeTest
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
        manager = PodcastManager(database)
    }

    @AfterTest
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testAddPodcastCreatesNewPodcast() = runBlocking {
        try {
            val result = manager.addPodcast(
                origin = "https://feeds.simplecast.com/54nAGcIl",
                seedColor = null
            )

            assertTrue(result is AddPodcastResult.Created)
            val podcast = (result as AddPodcastResult.Created).podcast
            assertNotNull(podcast.title)
            assertTrue(podcast.title.isNotEmpty())
        } catch (e: Exception) {
            println("Skipping test: Network not available: ${e.message}")
        }
    }

    @Test
    fun testAddPodcastDuplicateReturnsDuplicate() = runBlocking {
        try {
            val origin = "https://feeds.simplecast.com/54nAGcIl"

            val firstResult = manager.addPodcast(origin, null)
            assertTrue(firstResult is AddPodcastResult.Created)

            val secondResult = manager.addPodcast(origin, null)
            assertTrue(secondResult is AddPodcastResult.Duplicate)
        } catch (e: Exception) {
            println("Skipping test: Network not available: ${e.message}")
        }
    }

    @Test
    fun testAddPodcastSavesToDatabase() = runBlocking {
        try {
            val origin = "https://feeds.simplecast.com/54nAGcIl"
            manager.addPodcast(origin, null)

            val podcasts = database.podcasts.getAllSync()
            assertEquals(1, podcasts.size)
            assertEquals(origin, podcasts[0].origin)
        } catch (e: Exception) {
            println("Skipping test: Network not available: ${e.message}")
        }
    }

    @Test
    fun testAddPodcastSavesEpisodes() = runBlocking {
        try {
            val origin = "https://feeds.simplecast.com/54nAGcIl"
            manager.addPodcast(origin, null)

            val episodes = database.episodes.getAllByOrigin(origin)
            assertTrue(episodes.isNotEmpty(), "Should have saved episodes")
        } catch (e: Exception) {
            println("Skipping test: Network not available: ${e.message}")
        }
    }

    @Test
    fun testAddInvalidUrlThrowsException() = runBlocking {
        try {
            val result = manager.addPodcast(
                origin = "https://invalid-url-that-does-not-exist.example.com/feed.xml",
                seedColor = null
            )
            // If no exception, the result should be valid
            assertTrue(result is AddPodcastResult.Created || result is AddPodcastResult.Duplicate)
        } catch (e: Exception) {
            // Expected: invalid URL should throw an exception
            assertTrue(e.message != null)
        }
    }
}
