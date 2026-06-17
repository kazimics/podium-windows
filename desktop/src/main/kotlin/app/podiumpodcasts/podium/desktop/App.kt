package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.repository.SettingsRepository
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import com.russhwolf.settings.PreferencesSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val databaseDir = remember {
        val userHome = System.getProperty("user.home")
        java.io.File(userHome, ".podium")
    }

    val database = remember {
        databaseDir.mkdirs()
        val driver = app.podiumpodcasts.podium.data.createDatabaseDriver(java.io.File(databaseDir, "podium.db"))
        AppDatabase(driver)
    }

    val settingsRepository = remember {
        val prefs = java.util.prefs.Preferences.userRoot().node("podium")
        SettingsRepository(PreferencesSettings(prefs))
    }

    var currentScreen by remember { mutableStateOf("home") }

    PodiumTheme {
        when (currentScreen) {
            "home" -> HomeScreen(
                database = database,
                onNavigateToSettings = { currentScreen = "settings" }
            )
            "settings" -> SettingsScreen(
                settingsRepository = settingsRepository,
                onBack = { currentScreen = "home" }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(database: AppDatabase, onNavigateToSettings: () -> Unit) {
    var podcasts by remember { mutableStateOf(emptyList<app.podiumpodcasts.podium.data.model.Podcast>()) }

    LaunchedEffect(Unit) {
        podcasts = database.podcasts.getAllSync()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Podium") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        if (podcasts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No podcasts yet.\nAdd one to get started!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(podcasts) { podcast ->
                    ListItem(
                        headlineContent = { Text(podcast.fetchTitle()) },
                        supportingContent = { Text(podcast.author) },
                        leadingContent = { Icon(Icons.Default.Podcasts, null, Modifier.size(48.dp)) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
