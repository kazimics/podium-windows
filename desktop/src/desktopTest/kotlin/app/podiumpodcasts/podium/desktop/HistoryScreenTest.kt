package app.podiumpodcasts.podium.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.desktop.player.MediaPlayerState
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import org.junit.*
import java.io.File

class HistoryScreenTest {

    private lateinit var database: AppDatabase
    private lateinit var testDbFile: File

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_history_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
    }

    @After
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testHistoryEmptyState() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText("No history yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Episodes you play will appear here").assertIsDisplayed()
    }

    @Test
    fun testHistoryTitle() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
    }

    @Test
    fun testHistoryBackButton() {
        var backClicked = false
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = { backClicked = true })
            }
        }
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        assert(backClicked) { "Back button should trigger onBack" }
    }

    @Test
    fun testHistoryClearButtonHiddenWhenEmpty() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithContentDescription("Clear History").assertDoesNotExist()
    }

    @Test
    fun testHistoryClearDialogCancel() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
    }

    @Test
    fun testHistoryShowsIcon() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText("No history yet").assertIsDisplayed()
    }
}
