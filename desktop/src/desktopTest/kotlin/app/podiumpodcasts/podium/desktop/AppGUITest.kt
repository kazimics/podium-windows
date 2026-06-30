package app.podiumpodcasts.podium.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.desktop.player.MediaPlayerState
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import app.podiumpodcasts.podium.utils.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AppGUITest {

    private lateinit var database: AppDatabase
    private lateinit var testDbFile: File
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_gui_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
    }

    @After
    fun teardown() {
        database.close()
        testDbFile.delete()
        Dispatchers.resetMain()
    }

    // === Settings Screen Tests ===

    @Test
    fun testSettingsScreenContent() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["settings_title"]).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings["settings_export_opml"]).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings["settings_import_opml"]).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings["settings_about"]).assertIsDisplayed()
    }

    @Test
    fun testSettingsScreenVersion() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText(Strings.get("settings_version", "0.1.0")).assertIsDisplayed()
    }

    @Test
    fun testSettingsScreenAppName() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Podium - Podcast Player").assertIsDisplayed()
    }

    // === MiniPlayer Tests ===

    @Test
    fun testMiniPlayerHiddenWhenNothingPlaying() {
        composeTestRule.setContent {
            PodiumTheme {
                val playerState = MediaPlayerState()
                app.podiumpodcasts.podium.desktop.player.MiniPlayer(
                    state = playerState,
                    onExpand = {},
                    onShowQueue = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Unknown").assertDoesNotExist()
    }

    @Test
    fun testMiniPlayerVisibleWhenPlaying() {
        composeTestRule.setContent {
            PodiumTheme {
                val playerState = MediaPlayerState()
                playerState.play("https://example.com/audio.mp3", "Test Episode", null)
                app.podiumpodcasts.podium.desktop.player.MiniPlayer(
                    state = playerState,
                    onExpand = {},
                    onShowQueue = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Test Episode").assertIsDisplayed()
    }

    @Test
    fun testMiniPlayerShowsPlayPauseButton() {
        composeTestRule.setContent {
            PodiumTheme {
                val playerState = MediaPlayerState()
                playerState.play("https://example.com/audio.mp3", "Test", null)
                app.podiumpodcasts.podium.desktop.player.MiniPlayer(
                    state = playerState,
                    onExpand = {},
                    onShowQueue = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasContentDescription(Strings["player_play"]) or hasContentDescription(Strings["player_pause"])
        ).assertIsDisplayed()
    }

    @Test
    fun testMiniPlayerShowsSeekButtons() {
        composeTestRule.setContent {
            PodiumTheme {
                val playerState = MediaPlayerState()
                playerState.play("https://example.com/audio.mp3", "Test", null)
                app.podiumpodcasts.podium.desktop.player.MiniPlayer(
                    state = playerState,
                    onExpand = {},
                    onShowQueue = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(Strings["player_seek_back"]).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings["player_seek_forward"]).assertIsDisplayed()
    }

    // === FullPlayer Tests ===

    @Test
    fun testFullPlayerShowsTitle() {
        composeTestRule.setContent {
            PodiumTheme {
                val playerState = MediaPlayerState()
                playerState.play("https://example.com/audio.mp3", "Full Player Test", null)
                app.podiumpodcasts.podium.desktop.player.FullPlayer(
                    state = playerState,
                    onClose = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Full Player Test").assertIsDisplayed()
    }

    @Test
    fun testFullPlayerShowsControls() {
        composeTestRule.setContent {
            PodiumTheme {
                val playerState = MediaPlayerState()
                playerState.play("https://example.com/audio.mp3", "Test", null)
                app.podiumpodcasts.podium.desktop.player.FullPlayer(
                    state = playerState,
                    onClose = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasContentDescription(Strings["player_play"]) or hasContentDescription(Strings["player_pause"])
        ).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings["player_seek_back"]).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings["player_seek_forward"]).assertIsDisplayed()
    }

    @Test
    fun testFullPlayerShowsQueueButton() {
        composeTestRule.setContent {
            PodiumTheme {
                val playerState = MediaPlayerState()
                playerState.play("https://example.com/audio.mp3", "Test", null)
                app.podiumpodcasts.podium.desktop.player.FullPlayer(
                    state = playerState,
                    onClose = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(Strings["player_queue"]).assertIsDisplayed()
    }

    @Test
    fun testFullPlayerShowsSpeedSelector() {
        composeTestRule.setContent {
            PodiumTheme {
                val playerState = MediaPlayerState()
                playerState.play("https://example.com/audio.mp3", "Test", null)
                app.podiumpodcasts.podium.desktop.player.FullPlayer(
                    state = playerState,
                    onClose = {}
                )
            }
        }
        composeTestRule.onNodeWithText("1.0x").assertIsDisplayed()
    }

    @Test
    fun testFullPlayerShowsSleepTimerButton() {
        composeTestRule.setContent {
            PodiumTheme {
                val playerState = MediaPlayerState()
                playerState.play("https://example.com/audio.mp3", "Test", null)
                app.podiumpodcasts.podium.desktop.player.FullPlayer(
                    state = playerState,
                    onClose = {}
                )
            }
        }
        composeTestRule.onNodeWithText(Strings["player_timer"]).assertIsDisplayed()
    }

    // === History Screen Tests ===

    @Test
    fun testHistoryScreenEmpty() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["history_empty"]).assertIsDisplayed()
    }

    @Test
    fun testHistoryScreenTitle() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["history_title"]).assertIsDisplayed()
    }
}
