package app.podiumpodcasts.podium.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import org.junit.*
import java.io.File

class DiscoverScreenTest {

    private lateinit var database: AppDatabase
    private lateinit var testDbFile: File

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_discover_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
    }

    @After
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testDiscoverScreenDisplaysTitle() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Discover").assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenHasSearchField() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Search podcasts").assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenHasSearchButton() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Search podcasts").performTextInput("test")
        composeTestRule.onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenClearButtonAppearsWithInput() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Search podcasts").performTextInput("test query")
        composeTestRule.onNodeWithContentDescription("Clear").assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenClearButtonResetsQuery() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Search podcasts").performTextInput("test query")
        composeTestRule.onNodeWithContentDescription("Clear").performClick()
        composeTestRule.onNodeWithText("Search podcasts").assertExists()
        composeTestRule.onNodeWithContentDescription("Clear").assertDoesNotExist()
    }

    @Test
    fun testDiscoverScreenBackButton() {
        var backClicked = false
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = { backClicked = true })
            }
        }
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        assert(backClicked) { "Back button should trigger onBack" }
    }

    @Test
    fun testDiscoverScreenSearchButtonHiddenWithEmptyQuery() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Search").assertDoesNotExist()
    }

    @Test
    fun testDiscoverScreenTopBarShowsBackButton() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }
}
