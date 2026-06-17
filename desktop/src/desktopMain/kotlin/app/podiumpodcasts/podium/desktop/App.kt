package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import app.podiumpodcasts.podium.desktop.player.FullPlayer
import app.podiumpodcasts.podium.desktop.player.MediaPlayerState
import app.podiumpodcasts.podium.desktop.player.MiniPlayer
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

    val playerState = remember { MediaPlayerState() }

    var podcasts by remember { mutableStateOf(emptyList<Podcast>()) }
    var currentScreen by remember { mutableStateOf("home") }
    var selectedPodcast by remember { mutableStateOf<Podcast?>(null) }
    var episodes by remember { mutableStateOf(emptyList<PodcastEpisode>()) }
    var showFullPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        podcasts = database.podcasts.getAllSync()
    }

    PodiumTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when {
                    showFullPlayer -> FullPlayer(
                        state = playerState,
                        onClose = { showFullPlayer = false }
                    )
                    selectedPodcast != null -> PodcastDetailScreen(
                        podcast = selectedPodcast!!,
                        database = database,
                        playerState = playerState,
                        onBack = {
                            selectedPodcast = null
                            episodes = emptyList()
                        }
                    )
                    currentScreen == "home" -> HomeScreen(
                        podcasts = podcasts,
                        database = database,
                        onPodcastClick = { podcast ->
                            selectedPodcast = podcast
                        }
                    )
                    currentScreen == "settings" -> SettingsScreen(
                        onBack = { currentScreen = "home" }
                    )
                }
            }

            MiniPlayer(
                state = playerState,
                onExpand = { showFullPlayer = true }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    podcasts: List<Podcast>,
    database: AppDatabase,
    onPodcastClick: (Podcast) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Podium") },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Podcast")
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.RssFeed, null, Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No podcasts yet", style = MaterialTheme.typography.headlineSmall)
                    Text("Add one to get started!", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(podcasts) { podcast ->
                    ListItem(
                        headlineContent = { Text(podcast.title) },
                        supportingContent = { Text(podcast.author) },
                        leadingContent = {
                            Icon(Icons.Default.Podcasts, null, Modifier.size(48.dp))
                        },
                        modifier = Modifier.clickable { onPodcastClick(podcast) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastDetailScreen(
    podcast: Podcast,
    database: AppDatabase,
    playerState: MediaPlayerState,
    onBack: () -> Unit
) {
    var episodes by remember { mutableStateOf(emptyList<PodcastEpisode>()) }

    LaunchedEffect(podcast.origin) {
        episodes = database.episodes.getAllByOrigin(podcast.origin)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(podcast.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (episodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(episodes) { episode ->
                    ListItem(
                        headlineContent = { Text(episode.title) },
                        supportingContent = {
                            Text(
                                text = formatDuration(episode.duration),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            IconButton(onClick = {
                                playerState.play(
                                    url = episode.audioUrl,
                                    title = episode.title,
                                    artworkUrl = episode.imageUrl
                                )
                            }) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "Play")
                            }
                        },
                        modifier = Modifier.clickable {
                            playerState.play(
                                url = episode.audioUrl,
                                title = episode.title,
                                artworkUrl = episode.imageUrl
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}
