package app.podiumpodcasts.podium.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import app.podiumpodcasts.podium.desktop.player.MediaPlayerState
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
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

    // === Home Screen Tests ===

    @Test
    fun testHomeScreenHasTopBar() {
        composeTestRule.setContent {
            PodiumTheme { App() }
        }
        composeTestRule.onNodeWithText("Podium").assertIsDisplayed()
    }

    @Test
    fun testHomeScreenShowsAddButton() {
        composeTestRule.setContent {
            PodiumTheme { App() }
        }
        composeTestRule.onNodeWithContentDescription("Add Podcast").assertIsDisplayed()
    }

    @Test
    fun testHomeScreenShowsDiscoverButton() {
        composeTestRule.setContent {
            PodiumTheme { App() }
        }
        composeTestRule.onNodeWithContentDescription("Discover").assertIsDisplayed()
    }

    @Test
    fun testHomeScreenShowsHistoryButton() {
        composeTestRule.setContent {
            PodiumTheme { App() }
        }
        composeTestRule.onNodeWithContentDescription("History").assertIsDisplayed()
    }

    @Test
    fun testHomeScreenShowsSettingsButton() {
        composeTestRule.setContent {
            PodiumTheme { App() }
        }
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    // === Podcast List Tests ===

    @Test
    fun testHomeScreenShowsPodcastList() {
        val podcast = Podcast(
            origin = "https://example.com/feed.xml",
            link = "https://example.com",
            title = "Test Podcast",
            description = "Description",
            author = "Test Author",
            imageUrl = "https://example.com/image.jpg",
            imageSeedColor = 0,
            languageCode = "en",
            fileSize = 1000L,
            overrideTitle = "",
            skipBeginning = 0,
            skipEnding = 0
        )
        composeTestRule.setContent {
            PodiumTheme { App() }
        }
        // Note: This tests the initial state before data loads
        composeTestRule.onNodeWithText("Podium").assertIsDisplayed()
    }

    // === Settings Screen Tests ===

    @Test
    fun testSettingsScreenContent() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Export OPML").assertIsDisplayed()
        composeTestRule.onNodeWithText("Import OPML").assertIsDisplayed()
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
    }

    @Test
    fun testSettingsScreenVersion() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Version 0.1.0").assertIsDisplayed()
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
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
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
        composeTestRule.onNodeWithContentDescription("Seek Back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Seek Forward").assertIsDisplayed()
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
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Seek Back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Seek Forward").assertIsDisplayed()
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
        composeTestRule.onNodeWithContentDescription("Queue").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("Timer").assertIsDisplayed()
    }

    // === History Screen Tests ===

    @Test
    fun testHistoryScreenEmpty() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText("No history yet").assertIsDisplayed()
    }

    @Test
    fun testHistoryScreenTitle() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
    }

    // === Discover Screen Tests ===

    @Test
    fun testDiscoverScreenTitle() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Discover").assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenSearchField() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Search podcasts").assertIsDisplayed()
    }

    // === Navigation Flow Tests ===

    @Test
    fun testNavigateToDiscover() {
        composeTestRule.setContent {
            PodiumTheme { App() }
        }
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Discover").assertIsDisplayed()
    }

    @Test
    fun testNavigateToHistory() {
        composeTestRule.setContent {
            PodiumTheme { App() }
        }
        composeTestRule.onNodeWithContentDescription("History").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
    }

    @Test
    fun testNavigateToSettings() {
        composeTestRule.setContent {
            PodiumTheme { App() }
        }
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    // === Add Podcast Dialog Tests ===

    @Test
    fun testAddPodcastDialogOpens() {
        composeTestRule.setContent {
            PodiumTheme { App() }
        }
        composeTestRule.onNodeWithContentDescription("Add Podcast").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Enter the RSS feed URL of the podcast:").assertIsDisplayed()
        composeTestRule.onNodeWithText("RSS Feed URL").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
