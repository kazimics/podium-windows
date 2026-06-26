package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.model.PodcastHistory
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import app.podiumpodcasts.podium.desktop.player.MediaPlayerState
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    database: AppDatabase,
    playerState: MediaPlayerState,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var historyItems by remember { mutableStateOf(emptyList<Pair<PodcastHistory, PodcastEpisode?>>()) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        historyItems = database.history.getAllWithEpisode()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (historyItems.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No history yet", style = MaterialTheme.typography.headlineSmall)
                    Text("Episodes you play will appear here", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(historyItems) { (history, episode) ->
                    if (episode != null) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = episode.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Column {
                                    Text(
                                        text = episode.podcastTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatTimestamp(history.timestamp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingContent = {
                                IconButton(onClick = {
                                    playerState.play(
                                        url = episode.audioUrl,
                                        title = episode.title,
                                        artworkUrl = episode.imageUrl,
                                        durationMs = episode.duration * 1000L
                                    )
                                }) {
                                    AsyncImage(
                                        model = episode.imageUrl,
                                        contentDescription = episode.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        playerState.addToQueue(
                                            url = episode.audioUrl,
                                            title = episode.title,
                                            artworkUrl = episode.imageUrl,
                                            episodeId = episode.id
                                        )
                                    }) {
                                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to Queue")
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            database.history.delete(episode.id)
                                            historyItems = database.history.getAllWithEpisode()
                                        }
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove")
                                    }
                                }
                            },
                            modifier = Modifier.clickable {
                                playerState.play(
                                    url = episode.audioUrl,
                                    title = episode.title,
                                    artworkUrl = episode.imageUrl,
                                    durationMs = episode.duration * 1000L
                                )
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to clear all history?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        database.history.deleteAll()
                        historyItems = emptyList()
                    }
                    showClearDialog = false
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
