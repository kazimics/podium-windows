package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.api.apple.ApplePodcastClient
import app.podiumpodcasts.podium.api.model.PodcastPreviewModel
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.manager.AddPodcastResult
import app.podiumpodcasts.podium.manager.PodcastManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    database: AppDatabase,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val podcastManager = remember { PodcastManager(database) }
    val appleClient = remember { ApplePodcastClient() }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<PodcastPreviewModel>()) }
    var topPodcasts by remember { mutableStateOf(emptyList<PodcastPreviewModel>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var addedOrigins by remember { mutableStateOf(setOf<String>()) }

    DisposableEffect(Unit) {
        onDispose { appleClient.close() }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            topPodcasts = appleClient.topPodcasts.load()
        } catch (e: Exception) {
            errorMessage = "Failed to load top podcasts: ${e.message}"
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search podcasts") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            searchResults = emptyList()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            if (searchQuery.isNotEmpty()) {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                searchResults = appleClient.search.search(searchQuery)
                            } catch (e: Exception) {
                                errorMessage = "Search failed: ${e.message}"
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Search")
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                val podcasts = if (searchQuery.isNotEmpty()) searchResults else topPodcasts
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(podcasts) { preview ->
                        val isAdded = preview.fetchUrl in addedOrigins

                        ListItem(
                            headlineContent = { Text(preview.title) },
                            supportingContent = {
                                Column {
                                    Text(preview.author)
                                    if (preview.description.isNotEmpty()) {
                                        Text(
                                            text = preview.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2
                                        )
                                    }
                                }
                            },
                            leadingContent = {
                                Icon(Icons.Default.Podcasts, null, Modifier.size(48.dp))
                            },
                            trailingContent = {
                                if (!isAdded) {
                                    IconButton(onClick = {
                                        scope.launch {
                                            try {
                                                val result = podcastManager.addPodcast(preview.fetchUrl, null)
                                                if (result is AddPodcastResult.Created || result is AddPodcastResult.Duplicate) {
                                                    addedOrigins = addedOrigins + preview.fetchUrl
                                                }
                                            } catch (e: Exception) {
                                                errorMessage = "Failed to add: ${e.message}"
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Podcast")
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Added",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
