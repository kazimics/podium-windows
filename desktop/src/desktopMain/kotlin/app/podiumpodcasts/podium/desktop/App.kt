package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val database = remember {
        val userHome = System.getProperty("user.home")
        val dbDir = File(userHome, ".podium")
        dbDir.mkdirs()
        AppDatabase.build(File(dbDir, "podium.db"))
    }

    var podcasts by remember { mutableStateOf(emptyList<app.podiumpodcasts.podium.data.model.Podcast>()) }

    LaunchedEffect(Unit) {
        podcasts = database.podcasts.getAllSync()
    }

    PodiumTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Podium") })
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
}
