package app.podiumpodcasts.podium.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
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
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Discover").assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenShowsSubtitle() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Curated podcasts, handpicked for you.").assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenHasSearchField() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Search podcasts, episodes, topics...").assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenSearchShortcutBadge() {
        composeTestRule.setContent {
            PodiumTheme {
                DiscoverScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("\u2318K").assertIsDisplayed()
    }
}
