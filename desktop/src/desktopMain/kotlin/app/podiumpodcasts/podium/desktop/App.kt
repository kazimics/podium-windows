package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import app.podiumpodcasts.podium.desktop.player.FullPlayer
import app.podiumpodcasts.podium.desktop.player.MediaPlayerState
import app.podiumpodcasts.podium.desktop.player.MiniPlayer
import app.podiumpodcasts.podium.desktop.player.QueueDrawer
import app.podiumpodcasts.podium.manager.AddPodcastResult
import app.podiumpodcasts.podium.manager.DownloadManager
import app.podiumpodcasts.podium.manager.PodcastManager
import app.podiumpodcasts.podium.manager.SubscriptionManager
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import app.podiumpodcasts.podium.utils.Logger
import app.podiumpodcasts.podium.utils.Settings
import app.podiumpodcasts.podium.utils.Strings
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "App"

private fun logError(e: Throwable) {
    Logger.e(TAG, "Uncaught error: ${e.message}", e)
    try {
        val logFile = File(System.getProperty("user.home"), ".podium/crash.log")
        logFile.parentFile?.mkdirs()
        logFile.appendText(
            "[${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}] ${e.message}\n${e.stackTraceToString()}\n\n"
        )
    } catch (_: Exception) {}
}

private suspend fun playAndRecordHistory(
    database: AppDatabase,
    playerState: MediaPlayerState,
    episode: PodcastEpisode
) {
    Logger.i(TAG, "playAndRecordHistory: title=${episode.title}, url=${episode.audioUrl}")
    try {
        playerState.play(
            url = episode.audioUrl,
            title = episode.title,
            artworkUrl = episode.imageUrl,
            durationMs = episode.duration * 1000L
        )
        database.history.insert(episode.origin, episode.id)
        Logger.d(TAG, "History recorded for episode: ${episode.id}")
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to play episode: ${episode.title}", e)
        throw e
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val database = remember {
        try {
            val userHome = System.getProperty("user.home")
            val dbDir = File(userHome, ".podium")
            dbDir.mkdirs()
            Logger.i(TAG, "Initializing database at ${dbDir.absolutePath}")
            AppDatabase.build(File(dbDir, "podium.db"))
        } catch (e: Exception) {
            logError(e)
            throw e
        }
    }

    val podcastManager = remember { PodcastManager(database) }
    val subscriptionManager = remember { SubscriptionManager(database) }
    var downloadPath by remember { mutableStateOf(Settings.getDownloadPath()) }
    val downloadManager = remember(downloadPath) {
        val downloadsDir = File(downloadPath)
        downloadsDir.mkdirs()
        DownloadManager(database, downloadsDir)
    }
    val playerState = remember { MediaPlayerState() }
    val scope = rememberCoroutineScope()

    var podcasts by remember { mutableStateOf(emptyList<Podcast>()) }
    var currentScreen by remember { mutableStateOf("home") }
    var selectedPodcast by remember { mutableStateOf<Podcast?>(null) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var showQueueFromMini by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(mapOf<String, Pair<Long, Long>>()) }
    var downloadingEpisodes by remember { mutableStateOf(setOf<String>()) }
    var downloadVersion by remember { mutableIntStateOf(0) }
    var completedDownloads by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        Logger.d(TAG, "Loading podcasts from database")
        podcasts = database.podcasts.getAllSync()
        completedDownloads = database.downloads.getAllValidDownloadedIds()
        Logger.d(TAG, "Loaded ${podcasts.size} podcasts, ${completedDownloads.size} downloads")
    }

    val startDownload = { episode: PodcastEpisode, podcastTitle: String ->
        downloadingEpisodes = downloadingEpisodes + episode.id
        completedDownloads = completedDownloads - episode.id
        scope.launch {
            try {
                val result = downloadManager.downloadEpisode(
                    episodeId = episode.id,
                    audioUrl = episode.audioUrl,
                    origin = episode.origin,
                    episodeTitle = episode.title,
                    podcastTitle = podcastTitle
                ) { current, total ->
                    downloadProgress = downloadProgress + (episode.id to Pair(current, total))
                }
                if (result.isSuccess) {
                    completedDownloads = completedDownloads + episode.id
                }
            } finally {
                downloadingEpisodes = downloadingEpisodes - episode.id
                downloadProgress = downloadProgress - episode.id
                downloadVersion++
            }
        }
        Unit
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
                        downloadingEpisodes = downloadingEpisodes,
                        downloadProgress = downloadProgress,
                        downloadVersion = downloadVersion,
                        completedDownloads = completedDownloads,
                        onStartDownload = startDownload,
                        onBack = { selectedPodcast = null }
                    )
                    currentScreen == "home" -> HomeScreen(
                        podcasts = podcasts,
                        database = database,
                        subscriptionManager = subscriptionManager,
                        scope = scope,
                        onPodcastClick = { podcast -> selectedPodcast = podcast },
                        onAddPodcast = { showAddDialog = true },
                        onDiscover = { currentScreen = "discover" },
                        onHistory = { currentScreen = "history" },
                        onSettings = { currentScreen = "settings" },
                        onPodcastsChanged = { newPodcasts -> podcasts = newPodcasts }
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
                        onBack = { currentScreen = "home" },
                        onDownloadPathChanged = { newPath -> downloadPath = newPath },
                        onLanguageChanged = { /* Language change is handled by Settings */ }
                    )
                    currentScreen == "history" -> HistoryScreen(
                        database = database,
                        playerState = playerState,
                        onBack = { currentScreen = "home" }
                    )
                }
            }

            MiniPlayer(
                state = playerState,
                onExpand = { showFullPlayer = true },
                onShowQueue = { showQueueFromMini = true }
            )
        }
    }

    if (showQueueFromMini) {
        QueueDrawer(
            state = playerState,
            onDismiss = { showQueueFromMini = false }
        )
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
    database: AppDatabase,
    subscriptionManager: SubscriptionManager,
    scope: kotlinx.coroutines.CoroutineScope,
    onPodcastClick: (Podcast) -> Unit,
    onAddPodcast: () -> Unit,
    onDiscover: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onPodcastsChanged: (List<Podcast>) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var selectedPodcasts by remember { mutableStateOf(setOf<String>()) }
    var showBatchUnsubscribeDialog by remember { mutableStateOf(false) }
    var showUnsubscribeDialog by remember { mutableStateOf(false) }
    var podcastToUnsubscribe by remember { mutableStateOf<Podcast?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isEditing) {
                        Text("已选择 ${selectedPodcasts.size} 个")
                    } else {
                        Text(Strings["home_title"])
                    }
                },
                navigationIcon = {
                    if (isEditing) {
                        IconButton(onClick = {
                            isEditing = false
                            selectedPodcasts = emptySet()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "取消")
                        }
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            if (selectedPodcasts.size == podcasts.size) {
                                selectedPodcasts = emptySet()
                            } else {
                                selectedPodcasts = podcasts.map { it.origin }.toSet()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "全选")
                        }
                        IconButton(onClick = { showBatchUnsubscribeDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除选中")
                        }
                    } else {
                        IconButton(onClick = onDiscover) {
                            Icon(Icons.Default.Explore, contentDescription = Strings["nav_discover"])
                        }
                        IconButton(onClick = onHistory) {
                            Icon(Icons.Default.History, contentDescription = Strings["nav_history"])
                        }
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, contentDescription = Strings["nav_settings"])
                        }
                        IconButton(onClick = onAddPodcast) {
                            Icon(Icons.Default.Add, contentDescription = Strings["home_add_podcast"])
                        }
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
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
                    Text(Strings["home_empty"], style = MaterialTheme.typography.headlineSmall)
                    Text(Strings["home_empty_hint"], style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(onClick = onAddPodcast) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings["home_add_podcast"])
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
                            if (isEditing) {
                                Checkbox(
                                    checked = podcast.origin in selectedPodcasts,
                                    onCheckedChange = { checked ->
                                        selectedPodcasts = if (checked) {
                                            selectedPodcasts + podcast.origin
                                        } else {
                                            selectedPodcasts - podcast.origin
                                        }
                                    }
                                )
                            } else {
                                AsyncImage(
                                    model = podcast.imageUrl,
                                    contentDescription = podcast.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        },
                        trailingContent = {
                            if (!isEditing) {
                                IconButton(onClick = {
                                    podcastToUnsubscribe = podcast
                                    showUnsubscribeDialog = true
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "取消订阅")
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            if (!isEditing) {
                                onPodcastClick(podcast)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showUnsubscribeDialog && podcastToUnsubscribe != null) {
        AlertDialog(
            onDismissRequest = {
                showUnsubscribeDialog = false
                podcastToUnsubscribe = null
            },
            title = { Text("取消订阅") },
            text = { Text("确定要取消订阅 \"${podcastToUnsubscribe!!.title}\" 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        subscriptionManager.unsubscribe(podcastToUnsubscribe!!.origin)
                        onPodcastsChanged(database.podcasts.getAllSync())
                    }
                    showUnsubscribeDialog = false
                    podcastToUnsubscribe = null
                }) {
                    Text("取消订阅")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsubscribeDialog = false
                    podcastToUnsubscribe = null
                }) {
                    Text(Strings["dialog_cancel"])
                }
            }
        )
    }

    if (showBatchUnsubscribeDialog) {
        AlertDialog(
            onDismissRequest = { showBatchUnsubscribeDialog = false },
            title = { Text("批量取消订阅") },
            text = { Text("确定要取消订阅选中的 ${selectedPodcasts.size} 个播客吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        selectedPodcasts.forEach { origin ->
                            subscriptionManager.unsubscribe(origin)
                        }
                        onPodcastsChanged(database.podcasts.getAllSync())
                        selectedPodcasts = emptySet()
                        isEditing = false
                    }
                    showBatchUnsubscribeDialog = false
                }) {
                    Text("取消订阅")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchUnsubscribeDialog = false }) {
                    Text(Strings["dialog_cancel"])
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastDetailScreen(
    podcast: Podcast,
    database: AppDatabase,
    playerState: MediaPlayerState,
    downloadManager: DownloadManager,
    downloadingEpisodes: Set<String>,
    downloadProgress: Map<String, Pair<Long, Long>>,
    downloadVersion: Int,
    completedDownloads: Set<String>,
    onStartDownload: (PodcastEpisode, String) -> Unit,
    onBack: () -> Unit
) {
    var episodes by remember { mutableStateOf(emptyList<PodcastEpisode>()) }
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
                        Icon(Icons.Default.ArrowBack, contentDescription = Strings["nav_back"])
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
                    val isDownloaded = episode.id in completedDownloads

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
                                scope.launch {
                                    val downloadRecord = database.downloads.getByEpisodeId(episode.id)
                                    val url = if (downloadRecord != null && File(downloadRecord.filePath).exists()) {
                                        downloadRecord.filePath
                                    } else {
                                        episode.audioUrl
                                    }
                                    val epWithUrl = episode.copy(audioUrl = url)
                                    playAndRecordHistory(database, playerState, epWithUrl)
                                }
                            }) {
                                AsyncImage(
                                    model = episode.imageUrl ?: podcast.imageUrl,
                                    contentDescription = episode.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        },
                        trailingContent = {
                            val isDownloaded = episode.id in completedDownloads
                            val isDownloading = episode.id in downloadingEpisodes
                            val progress = downloadProgress[episode.id]

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    scope.launch {
                                        val downloadRecord = database.downloads.getByEpisodeId(episode.id)
                                        val url = if (downloadRecord != null && File(downloadRecord.filePath).exists()) {
                                            downloadRecord.filePath
                                        } else {
                                            episode.audioUrl
                                        }
                                        playerState.addToQueue(
                                            url = url,
                                            title = episode.title,
                                            artworkUrl = episode.imageUrl,
                                            episodeId = episode.id,
                                            isDownloaded = episode.id in completedDownloads
                                        )
                                    }
                                }) {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = Strings["episode_add_to_queue"])
                                }

                                Box(
                                    modifier = Modifier.size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDownloaded) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = Strings["episode_downloaded"],
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (isDownloading) {
                                        val fraction = if (progress != null && progress.second > 0) {
                                            progress.first.toFloat() / progress.second
                                        } else 0f
                                        CircularProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        IconButton(onClick = {
                                            onStartDownload(episode, podcast.title)
                                        }) {
                                            Icon(Icons.Default.Download, contentDescription = Strings["episode_download"])
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            scope.launch {
                                val downloadRecord = database.downloads.getByEpisodeId(episode.id)
                                val url = if (downloadRecord != null && File(downloadRecord.filePath).exists()) {
                                    downloadRecord.filePath
                                } else {
                                    episode.audioUrl
                                }
                                val epWithUrl = episode.copy(audioUrl = url)
                                playAndRecordHistory(database, playerState, epWithUrl)
                            }
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
        title = { Text(Strings["home_add_podcast"]) },
        text = {
            Column {
                Text(Strings["add_podcast_hint"])
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
                Text(Strings["dialog_ok"])
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings["dialog_cancel"])
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
