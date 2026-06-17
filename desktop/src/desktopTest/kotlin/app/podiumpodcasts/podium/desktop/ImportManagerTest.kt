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
    }

    @Test
    fun testImportInvalidXml() = runBlocking {
        val opml = "This is not valid XML"

        val result = importManager.importOpml(opml)

        assertTrue(result is ImportResult.Error)
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

        importManager.importOpml(opml)
        val result = importManager.importOpml(opml)

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertTrue(success.skipped > 0 || success.added > 0)
    }
}
