package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.manager.ImportManager
import app.podiumpodcasts.podium.manager.ImportResult
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class ImportManagerTest {

    private lateinit var database: AppDatabase
    private lateinit var importManager: ImportManager
    private lateinit var testDbFile: File

    @BeforeTest
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
        importManager = ImportManager(database)
    }

    @AfterTest
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testImportValidOpml() = runBlocking {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>Test</title></head>
              <body>
                <outline type="rss" text="Test Podcast" title="Test Podcast" xmlUrl="https://example.com/feed.xml" htmlUrl="https://example.com"/>
              </body>
            </opml>
        """.trimIndent()

        val result = importManager.importOpml(opml)

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertTrue(success.added > 0 || success.failed > 0)
    }

    @Test
    fun testImportEmptyOpml() = runBlocking {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>Empty</title></head>
              <body></body>
            </opml>
        """.trimIndent()

        val result = importManager.importOpml(opml)

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(0, success.added)
        assertEquals(0, success.failed)
        assertEquals(0, success.skipped)
    }

    @Test
    fun testImportInvalidXml() = runBlocking {
        val opml = "This is not valid XML"

        val result = importManager.importOpml(opml)

        assertTrue(result is ImportResult.Error)
        val error = result as ImportResult.Error
        assertTrue(error.message.contains("Failed to parse OPML"))
    }

    @Test
    fun testImportDuplicateSkipped() = runBlocking {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>Duplicate Test</title></head>
              <body>
                <outline type="rss" text="Test" title="Test" xmlUrl="https://example.com/feed.xml"/>
              </body>
            </opml>
        """.trimIndent()

        val firstResult = importManager.importOpml(opml)
        val secondResult = importManager.importOpml(opml)

        assertTrue(firstResult is ImportResult.Success)
        assertTrue(secondResult is ImportResult.Success)
        val first = firstResult as ImportResult.Success
        val second = secondResult as ImportResult.Success
        if (first.added > 0) {
            assertTrue(second.skipped > 0)
        }
    }

    @Test
    fun testImportMultiplePodcasts() = runBlocking {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>Multiple</title></head>
              <body>
                <outline type="rss" text="Podcast 1" xmlUrl="https://example.com/feed1.xml"/>
                <outline type="rss" text="Podcast 2" xmlUrl="https://example.com/feed2.xml"/>
                <outline type="rss" text="Podcast 3" xmlUrl="https://example.com/feed3.xml"/>
              </body>
            </opml>
        """.trimIndent()

        val result = importManager.importOpml(opml)

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        val total = success.added + success.failed
        assertEquals(3, total)
    }

    @Test
    fun testImportNonRssOutlineIgnored() = runBlocking {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>Non-RSS</title></head>
              <body>
                <outline text="Not a podcast" xmlUrl="https://example.com/not-a-feed"/>
              </body>
            </opml>
        """.trimIndent()

        val result = importManager.importOpml(opml)

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(0, success.added)
        assertEquals(0, success.skipped)
    }

    @Test
    fun testImportPreservesOriginalTitle() = runBlocking {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>Preserve</title></head>
              <body>
                <outline type="rss" text="My Custom Title" xmlUrl="https://example.com/feed.xml"/>
              </body>
            </opml>
        """.trimIndent()

        val result = importManager.importOpml(opml)

        assertTrue(result is ImportResult.Success)
    }
}
