package app.podiumpodcasts.podium.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.manager.SubscriptionManager
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
class DiscoverScreenTest {

    private lateinit var database: AppDatabase
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var testDbFile: File
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_discover_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
        subscriptionManager = SubscriptionManager(database)
    }

    @After
    fun teardown() {
        database.close()
        testDbFile.delete()
        Dispatchers.resetMain()
    }

    @Test
    fun testDiscoverScreenDisplaysTitle() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, subscriptionManager = subscriptionManager, onSubscribed = {}, onBack = {}, onPlayLatestEpisode = {}, onShowDetail = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["discover_title"]).assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenShowsSubtitle() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, subscriptionManager = subscriptionManager, onSubscribed = {}, onBack = {}, onPlayLatestEpisode = {}, onShowDetail = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["discover_subtitle"]).assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenHasSearchField() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, subscriptionManager = subscriptionManager, onSubscribed = {}, onBack = {}, onPlayLatestEpisode = {}, onShowDetail = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["discover_search_placeholder"]).assertIsDisplayed()
    }

}
