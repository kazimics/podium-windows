package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Cursor
import app.podiumpodcasts.podium.api.apple.ApplePodcastClient
import app.podiumpodcasts.podium.api.model.PodcastPreviewModel
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.manager.AddPodcastResult
import app.podiumpodcasts.podium.manager.PodcastManager
import app.podiumpodcasts.podium.utils.Logger
import app.podiumpodcasts.podium.utils.Strings
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

private const val TAG = "DiscoverScreen"

@Composable
fun DiscoverScreen(
    database: AppDatabase,
    onBack: () -> Unit
) {
    val colors = PodiumTheme.colors
    val scope = rememberCoroutineScope()
    val podcastManager = remember { PodcastManager(database) }
    val appleClient = remember { ApplePodcastClient() }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<PodcastPreviewModel>()) }
    var topPodcasts by remember { mutableStateOf(emptyList<PodcastPreviewModel>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var subscribedOrigins by remember { mutableStateOf(setOf<String>()) }
    var subscribingOrigins by remember { mutableStateOf(setOf<String>()) }

    DisposableEffect(Unit) {
        onDispose { appleClient.close() }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        Logger.i(TAG, "Loading top podcasts and subscriptions")
        try {
            val dbOrigins = database.podcasts.getAllOrigins()
            topPodcasts = appleClient.topPodcasts.load()
            val itunesIds = topPodcasts
                .filter { it.fetchUrl.startsWith("itunes-lookup:") }
                .mapNotNull { it.fetchUrl.removePrefix("itunes-lookup:").toLongOrNull() }
            val resolvedUrls = appleClient.lookup.batchLookupFeedUrls(itunesIds).values.toSet()
            subscribedOrigins = dbOrigins + resolvedUrls
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load data", e)
            errorMessage = "Failed to load: ${e.message}"
        }
        isLoading = false
    }

    val doSearch: () -> Unit = {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                searchResults = appleClient.search.search(searchQuery)
            } catch (e: Exception) {
                Logger.e(TAG, "Search failed", e)
                errorMessage = "Search failed: ${e.message}"
            }
            isLoading = false
        }
    }

    val subscribe: (PodcastPreviewModel) -> Unit = { preview ->
        scope.launch {
            subscribingOrigins = subscribingOrigins + preview.fetchUrl
            try {
                val result = podcastManager.addPodcast(preview.fetchUrl, null)
                if (result is AddPodcastResult.Created || result is AddPodcastResult.Duplicate) {
                    subscribedOrigins = subscribedOrigins + preview.fetchUrl
                }
            } catch (e: Exception) {
                errorMessage = "Failed to add: ${e.message}"
            } finally {
                subscribingOrigins = subscribingOrigins - preview.fetchUrl
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background)
    ) {
        // ── Header: Title + Search ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 28.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Discover",
                    color = colors.textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Curated podcasts, handpicked for you.",
                    color = colors.textMuted,
                    fontSize = 14.sp
                )
            }

            // Search bar
            Surface(
                modifier = Modifier.width(320.dp).height(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = colors.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search podcasts, episodes, topics...",
                                color = colors.textDisabled,
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = colors.textPrimary,
                                fontSize = 13.sp
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(colors.accent)
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(14.dp)
                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                .clickable {
                                    searchQuery = ""
                                    searchResults = emptyList()
                                }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = colors.elevated
                    ) {
                        Text(
                            text = "\u2318K",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Search button ──
        if (searchQuery.isNotEmpty()) {
            TextButton(
                onClick = { doSearch() },
                modifier = Modifier.padding(start = 32.dp)
            ) {
                Text(Strings["discover_search"], color = colors.accent)
            }
        }

        // ── Content ──
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage!!, color = colors.danger)
                }
            }
            else -> {
                val podcasts = if (searchQuery.isNotEmpty()) searchResults else topPodcasts
                if (podcasts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(Strings["discover_search_hint"], color = colors.textMuted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // ── Featured card (first podcast) ──
                        if (searchQuery.isEmpty() && podcasts.isNotEmpty()) {
                            item {
                                var featuredIndex by remember { mutableIntStateOf(0) }
                                val featured = podcasts[featuredIndex % podcasts.size]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // arrows are inside FeaturedCard
                                }
                                FeaturedCard(
                                    podcast = featured,
                                    isSubscribed = featured.fetchUrl in subscribedOrigins,
                                    onSubscribe = { subscribe(featured) },
                                    onPrevious = { featuredIndex = if (featuredIndex > 0) featuredIndex - 1 else podcasts.size - 1 },
                                    onNext = { featuredIndex = (featuredIndex + 1) % podcasts.size }
                                )
                            }
                        }

                        // ── Trending This Week ──
                        if (searchQuery.isEmpty() && podcasts.size > 1) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                SectionHeader(title = "Trending This Week")
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    podcasts.drop(1).take(10).forEach { podcast ->
                                        PodcastCard(
                                            podcast = podcast,
                                            onClick = { /* TODO */ }
                                        )
                                    }
                                }
                            }
                        }

                        // ── New Episodes / All results ──
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            SectionHeader(
                                title = if (searchQuery.isEmpty()) "New Episodes" else "Results"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        val listItems = if (searchQuery.isEmpty()) podcasts.drop(1) else podcasts
                        items(listItems) { podcast ->
                            EpisodeRow(
                                podcast = podcast,
                                isSubscribed = podcast.fetchUrl in subscribedOrigins,
                                isSubscribing = podcast.fetchUrl in subscribingOrigins,
                                onSubscribe = { subscribe(podcast) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Featured Card ──
@Composable
private fun FeaturedCard(
    podcast: PodcastPreviewModel,
    isSubscribed: Boolean,
    onSubscribe: () -> Unit,
    onShowDetail: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {}
) {
    val colors = PodiumTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 0.dp)
            .height(280.dp),
        shape = RoundedCornerShape(18.dp),
        color = colors.elevated
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Navigation arrows — top right
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.6f))
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable { onPrevious() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.6f))
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Content: Cover + Text
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover — square, rounded
                AsyncImage(
                    model = podcast.imageUrl,
                    contentDescription = podcast.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Text content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "FEATURED",
                        color = colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = podcast.title,
                        color = colors.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (podcast.description.isNotEmpty()) {
                        Text(
                            text = podcast.description,
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            maxLines = 3,
                            lineHeight = 19.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Three action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Latest Episode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.accent,
                            modifier = Modifier
                                .height(38.dp)
                                .clickable { onSubscribe() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = colors.background,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Latest Episode",
                                    color = colors.background,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Add to playlist
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .clickable { onSubscribe() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // More / Detail
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .clickable { onShowDetail() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = "More",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Section Header ──
@Composable
private fun SectionHeader(title: String) {
    val colors = PodiumTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Show All",
            color = colors.accent,
            fontSize = 13.sp,
            modifier = Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
        )
    }
}

// ── Podcast Card (horizontal scroll) ──
@Composable
private fun PodcastCard(
    podcast: PodcastPreviewModel,
    onClick: () -> Unit
) {
    val colors = PodiumTheme.colors
    Column(
        modifier = Modifier
            .width(150.dp)
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = podcast.imageUrl,
            contentDescription = podcast.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = podcast.title,
            color = colors.textPrimary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = podcast.author,
            color = colors.textMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

// ── Episode Row ──
@Composable
private fun EpisodeRow(
    podcast: PodcastPreviewModel,
    isSubscribed: Boolean,
    isSubscribing: Boolean,
    onSubscribe: () -> Unit
) {
    val colors = PodiumTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .padding(horizontal = 32.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cover
        AsyncImage(
            model = podcast.imageUrl,
            contentDescription = podcast.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        // Title + Author + Description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = podcast.title,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = podcast.author,
                color = colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
            if (podcast.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = podcast.description,
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        // Subscribe button
        when {
            isSubscribed -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = Strings["discover_added"],
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            isSubscribing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = colors.accent
                )
            }
            else -> {
                IconButton(onClick = onSubscribe, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = Strings["discover_add"],
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
