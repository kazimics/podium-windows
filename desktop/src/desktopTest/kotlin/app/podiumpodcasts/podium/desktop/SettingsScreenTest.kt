package app.podiumpodcasts.podium.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import org.junit.*
import java.io.File

class SettingsScreenTest {

    private lateinit var database: AppDatabase
    private lateinit var testDbFile: File

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_settings_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
    }

    @After
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testSettingsTitle() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun testSettingsVersion() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Version 0.1.0").assertIsDisplayed()
    }

    @Test
    fun testSettingsAppName() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Podium - Podcast Player").assertIsDisplayed()
    }

    @Test
    fun testSettingsExportButton() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Export OPML").assertIsDisplayed()
        composeTestRule.onNodeWithText("Export").assertIsDisplayed()
    }

    @Test
    fun testSettingsImportButton() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Import OPML").assertIsDisplayed()
        composeTestRule.onNodeWithText("Import").assertIsDisplayed()
    }

    @Test
    fun testSettingsBackButton() {
        var backClicked = false
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = { backClicked = true })
            }
        }
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        assert(backClicked) { "Back button should trigger onBack" }
    }

    @Test
    fun testSettingsDataSection() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Data").assertIsDisplayed()
    }

    @Test
    fun testSettingsAboutSection() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
    }

    @Test
    fun testSettingsExportDescription() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Export your podcast subscriptions as OPML").assertIsDisplayed()
    }

    @Test
    fun testSettingsImportDescription() {
        composeTestRule.setContent {
            PodiumTheme {
                SettingsScreen(database = database, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Import podcast subscriptions from OPML file").assertIsDisplayed()
    }
}
