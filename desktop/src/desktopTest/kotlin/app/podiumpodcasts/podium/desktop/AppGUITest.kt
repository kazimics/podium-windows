package app.podiumpodcasts.podium.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.desktop.player.MediaPlayerState
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import org.junit.*
import java.io.File

class AppGUITest {

    private lateinit var database: AppDatabase
    private lateinit var testDbFile: File

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_gui_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
    }

    @After
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testHomeScreenShowsEmptyState() {
        composeTestRule.setContent {
            PodiumTheme {
                App()
            }
        }

        composeTestRule.onNodeWithText("No podcasts yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add one to get started!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Podcast").assertIsDisplayed()
    }

    @Test
    fun testSettingsScreenContent() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(
                    database = database,
                    onBack = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Export OPML").assertIsDisplayed()
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
    }

    @Test
    fun testMiniPlayerHiddenWhenNothingPlaying() {
        composeTestRule.setContent {
            PodiumTheme {
                val playerState = MediaPlayerState()
                app.podiumpodcasts.podium.desktop.player.MiniPlayer(
                    state = playerState,
                    onExpand = { }
                )
            }
        }

        // MiniPlayer returns early when currentUrl is null, so no content is rendered
        composeTestRule.onNodeWithText("Unknown").assertDoesNotExist()
    }
}
