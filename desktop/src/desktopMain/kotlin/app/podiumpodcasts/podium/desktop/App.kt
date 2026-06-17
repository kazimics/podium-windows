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
import app.podiumpodcasts.podium.manager.AddPodcastResult
import app.podiumpodcasts.podium.manager.DownloadManager
import app.podiumpodcasts.podium.manager.PodcastManager
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import kotlinx.coroutines.launch
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

    val podcastManager = remember { PodcastManager(database) }
    val downloadManager = remember {
        val downloadsDir = File(System.getProperty("user.home"), ".podium/downloads")
        downloadsDir.mkdirs()
        DownloadManager(database, downloadsDir)
    }
    val playerState = remember { MediaPlayerState() }
    val scope = rememberCoroutineScope()

    var podcasts by remember { mutableStateOf(emptyList<Podcast>()) }
    var currentScreen by remember { mutableStateOf("home") }
    var selectedPodcast by remember { mutableStateOf<Podcast?>(null) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }

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
                        downloadManager = downloadManager,
                        onBack = { selectedPodcast = null }
                    )
                    currentScreen == "home" -> HomeScreen(
                        podcasts = podcasts,
                        onPodcastClick = { podcast -> selectedPodcast = podcast },
                        onAddPodcast = { showAddDialog = true },
                        onDiscover = { currentScreen = "discover" },
                        onSettings = { currentScreen = "settings" }
                    )
                    currentScreen == "discover" -> DiscoverScreen(
                        database = database,
                        onBack = {
                            currentScreen = "home"
                            scope.launch { podcasts = database.podcasts.getAllSync() }
                        }
                    )
                    currentScreen == "settings" -> SettingsScreen(
                        database = database,
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

    if (showAddDialog) {
        AddPodcastDialog(
            onDismiss = {
                showAddDialog = false
                addError = null
            },
            onConfirm = { url ->
                showAddDialog = false
                scope.launch {
                    try {
                        when (val result = podcastManager.addPodcast(url, null)) {
                            is AddPodcastResult.Created -> {
                                podcasts = database.podcasts.getAllSync()
                            }
                            is AddPodcastResult.Duplicate -> {
                                addError = "Podcast already exists: ${result.duplicate.title}"
                                showAddDialog = true
                            }
                        }
                    } catch (e: Exception) {
                        addError = "Failed to add podcast: ${e.message}"
                        showAddDialog = true
                    }
                }
            },
            error = addError
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    podcasts: List<Podcast>,
    onPodcastClick: (Podcast) -> Unit,
    onAddPodcast: () -> Unit,
    onDiscover: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Podium") },
                actions = {
                    IconButton(onClick = onDiscover) {
                        Icon(Icons.Default.Explore, contentDescription = "Discover")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onAddPodcast) {
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
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(onClick = onAddPodcast) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Podcast")
                    }
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
    downloadManager: DownloadManager,
    onBack: () -> Unit
) {
    var episodes by remember { mutableStateOf(emptyList<PodcastEpisode>()) }
    var downloadingEpisodes by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

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
                    val isDownloaded = remember(episode) {
                        downloadManager.getDownloadFile(episode.origin, episode.audioUrl).exists()
                    }
                    val isDownloading = episode.id in downloadingEpisodes

                    ListItem(
                        headlineContent = { Text(episode.title) },
                        supportingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatDuration(episode.duration),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (isDownloaded) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Downloaded",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        leadingContent = {
                            IconButton(onClick = {
                                val audioFile = downloadManager.getDownloadFile(episode.origin, episode.audioUrl)
                                val url = if (audioFile.exists()) audioFile.absolutePath else episode.audioUrl
                                playerState.play(
                                    url = url,
                                    title = episode.title,
                                    artworkUrl = episode.imageUrl
                                )
                            }) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "Play")
                            }
                        },
                        trailingContent = {
                            if (!isDownloaded && !isDownloading) {
                                IconButton(onClick = {
                                    scope.launch {
                                        downloadingEpisodes = downloadingEpisodes + episode.id
                                        try {
                                            downloadManager.downloadEpisode(
                                                episodeId = episode.id,
                                                audioUrl = episode.audioUrl,
                                                origin = episode.origin
                                            )
                                        } finally {
                                            downloadingEpisodes = downloadingEpisodes - episode.id
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Download, contentDescription = "Download")
                                }
                            } else if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            val audioFile = downloadManager.getDownloadFile(episode.origin, episode.audioUrl)
                            val url = if (audioFile.exists()) audioFile.absolutePath else episode.audioUrl
                            playerState.play(
                                url = url,
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

@Composable
private fun AddPodcastDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    error: String? = null
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Podcast") },
        text = {
            Column {
                Text("Enter the RSS feed URL of the podcast:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("RSS Feed URL") },
                    placeholder = { Text("https://example.com/feed.xml") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        onConfirm(url)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
