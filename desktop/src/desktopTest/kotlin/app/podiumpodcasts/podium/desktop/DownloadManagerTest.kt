package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.manager.DownloadManager
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class DownloadManagerTest {

    private lateinit var database: AppDatabase
    private lateinit var downloadManager: DownloadManager
    private lateinit var testDbFile: File
    private lateinit var testDownloadsDir: File

    @BeforeTest
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        testDownloadsDir = File(System.getProperty("java.io.tmpdir"), "podium_test_downloads_${System.currentTimeMillis()}")
        testDownloadsDir.mkdirs()
        testDownloadsDir.deleteOnExit()
        database = AppDatabase.build(testDbFile)
        downloadManager = DownloadManager(database, testDownloadsDir)
    }

    @AfterTest
    fun teardown() {
        database.close()
        testDbFile.delete()
        testDownloadsDir.deleteRecursively()
    }

    @Test
    fun testGetDownloadFileReturnsCorrectPath() {
        val origin = "https://example.com/feed.xml"
        val audioUrl = "https://example.com/audio.mp3"

        val file = downloadManager.getDownloadFile(origin, audioUrl)

        assertTrue(file.absolutePath.contains(testDownloadsDir.absolutePath))
        assertTrue(file.name.isNotEmpty())
    }

    @Test
    fun testGetDownloadFileConsistentPath() {
        val origin = "https://example.com/feed.xml"
        val audioUrl = "https://example.com/audio.mp3"

        val file1 = downloadManager.getDownloadFile(origin, audioUrl)
        val file2 = downloadManager.getDownloadFile(origin, audioUrl)

        assertEquals(file1.absolutePath, file2.absolutePath)
    }

    @Test
    fun testGetDownloadFileDifferentOrigins() {
        val file1 = downloadManager.getDownloadFile("https://example.com/feed1.xml", "https://example.com/audio.mp3")
        val file2 = downloadManager.getDownloadFile("https://example.com/feed2.xml", "https://example.com/audio.mp3")

        assertNotEquals(file1.absolutePath, file2.absolutePath)
    }

    @Test
    fun testGetDownloadFileDifferentUrls() {
        val file1 = downloadManager.getDownloadFile("https://example.com/feed.xml", "https://example.com/audio1.mp3")
        val file2 = downloadManager.getDownloadFile("https://example.com/feed.xml", "https://example.com/audio2.mp3")

        assertNotEquals(file1.absolutePath, file2.absolutePath)
    }

    @Test
    fun testDownloadAndDeleteEpisode() = runBlocking {
        try {
            val origin = "https://example.com/feed.xml"
            val audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"

            val result = downloadManager.downloadEpisode("test-episode", audioUrl, origin)

            if (result.isSuccess) {
                val file = result.getOrNull()!!
                assertTrue(file.exists(), "Downloaded file should exist")
                assertTrue(file.length() > 0, "Downloaded file should not be empty")

                downloadManager.deleteEpisodeDownload("test-episode", origin, audioUrl)
                assertFalse(file.exists(), "File should be deleted after deleteEpisodeDownload")
            }
        } catch (e: Exception) {
            println("Skipping test: Network not available: ${e.message}")
        }
    }

    @Test
    fun testGetDownloadFileWhenNotDownloaded() {
        val file = downloadManager.getDownloadFile(
            "https://example.com/feed.xml",
            "https://example.com/nonexistent.mp3"
        )

        assertFalse(file.exists())
    }

    @Test
    fun testDeleteNonExistentFile() = runBlocking {
        downloadManager.deleteEpisodeDownload(
            "nonexistent",
            "https://example.com/feed.xml",
            "https://example.com/audio.mp3"
        )
    }
}
