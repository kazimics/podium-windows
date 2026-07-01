package app.podiumpodcasts.podium.desktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import app.podiumpodcasts.podium.ui.theme.DesignTokens
import app.podiumpodcasts.podium.ui.theme.PodiumBackground
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import app.podiumpodcasts.podium.utils.Strings
import coil3.compose.AsyncImage
import java.awt.Cursor

// ── Design Tokens: button.primary ──
private val PrimaryButtonGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFC5976F),
        Color(0xFFBF936C),
        Color(0xFFB1845F)
    ),
    startY = 0f,
    endY = 48f
)
private val PrimaryButtonBorder = Color(0x14FFFFFF)
private val PrimaryButtonText = Color(0xFFFFF8F3)
private val PrimaryButtonIcon = Color.White
private val PrimaryButtonInnerHighlight = Brush.verticalGradient(
    colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
    startY = 0f, endY = 56f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    state: MediaPlayerState,
    onExpand: () -> Unit,
    onShowQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PodiumTheme.colors
    val progress by remember { derivedStateOf { state.getProgress() } }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    var showSpeedMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentPosition, state.duration) {
        if (!isDragged && state.duration > 0) {
            sliderPosition = state.currentPosition.toFloat() / state.duration
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PodiumBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
        ) {
            Box(
                modifier = Modifier
                    .width(DesignTokens.Sidebar.Width)
                    .fillMaxHeight()
                    .background(colors.surface)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                    .height(88.dp)
                    .shadow(
                        8.dp,
                        RoundedCornerShape(18.dp),
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = Color.Black.copy(alpha = 0.4f)
                    )
                    .border(DesignTokens.Border.Width, colors.border, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(DesignTokens.Card.Gradient)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ── Left: Cover + Title + Podcast ──
                    Row(
                        modifier = Modifier.weight(0.9f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AsyncImage(
                            model = state.currentArtworkUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Column {
                            Text(
                                text = state.currentTitle ?: Strings["player_no_playback"],
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.currentSubtitle != null) {
                                Text(
                                    text = state.currentSubtitle!!,
                                    color = colors.textMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // ── Center: Speed + Rewind + Play + Forward ──
                    Row(
                        modifier = Modifier.width(260.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                    .clickable { showSpeedMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${state.playbackSpeed}x",
                                    color = colors.textMuted,
                                    fontSize = 14.sp
                                )
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text("${speed}x") },
                                        onClick = {
                                            state.changePlaybackSpeed(speed)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { state.seekBack() },
                            modifier = Modifier.size(48.dp).pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        ) {
                            Icon(
                                Icons.Default.Replay10,
                                contentDescription = Strings["player_seek_back"],
                                tint = colors.textMuted,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
                                .border(1.dp, PrimaryButtonBorder, CircleShape)
                                .background(PrimaryButtonGradient)
                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                .clickable(enabled = state.currentUrl != null) { state.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.matchParentSize().background(PrimaryButtonInnerHighlight))
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) Strings["player_pause"] else Strings["player_play"],
                                tint = PrimaryButtonIcon,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(
                            onClick = { state.seekForward() },
                            modifier = Modifier.size(40.dp).pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        ) {
                            Icon(
                                Icons.Default.Forward10,
                                contentDescription = Strings["player_seek_forward"],
                                tint = colors.textMuted,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // ── Right: Time + Slider + Volume + Queue + Fullscreen ──
                    Row(
                        modifier = Modifier.weight(1f).padding(start = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatTime(state.currentPosition),
                            color = colors.textMuted,
                            fontSize = 12.sp
                        )

                        Slider(
                            value = sliderPosition,
                            onValueChange = { sliderPosition = it },
                            onValueChangeFinished = {
                                val pos = (sliderPosition * state.duration).toLong()
                                state.seek(pos)
                            },
                            modifier = Modifier.weight(1f).height(20.dp),
                            thumb = {
                                SliderDefaults.Thumb(
                                    interactionSource = interactionSource,
                                    colors = SliderDefaults.colors(thumbColor = Color.White),
                                    thumbSize = DpSize(12.dp, 12.dp),
                                    modifier = Modifier.offset(y = 2.dp)
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.height(6.dp),
                                    thumbTrackGapSize = 0.dp,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = colors.accent,
                                        inactiveTrackColor = colors.elevated
                                    )
                                )
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.elevated
                            ),
                            interactionSource = interactionSource
                        )

                        Text(
                            text = formatTime(state.duration),
                            color = colors.textMuted,
                            fontSize = 12.sp
                        )

                        IconButton(
                            onClick = { state.toggleMute() },
                            modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        ) {
                            Icon(
                                if (state.volume > 0) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = if (state.volume > 0) Strings["player_mute"] else Strings["player_unmute"],
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onShowQueue,
                            modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        ) {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = Strings["player_queue"],
                                tint = colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onExpand,
                            modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Expand",
                                tint = colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayer(
    state: MediaPlayerState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentPosition, state.duration) {
        if (!isDragging && state.duration > 0) {
            sliderPosition = state.currentPosition.toFloat() / state.duration
        }
    }

    val colors = PodiumTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = Strings["player_close"], tint = colors.textMuted)
            }

            Text(
                text = state.currentTitle ?: Strings["player_now_playing"],
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            IconButton(onClick = { showQueue = true }) {
                Icon(Icons.Default.QueueMusic, contentDescription = Strings["player_queue"], tint = colors.textMuted)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                isDragging = true
            },
            onValueChangeFinished = {
                isDragging = false
                val position = (sliderPosition * state.duration).toLong()
                state.seek(position)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(state.currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
            Text(
                text = formatTime(state.duration),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { state.playPrevious() }) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = Strings["player_previous"],
                    tint = colors.textMuted,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { state.seekBack() }) {
                Icon(
                    Icons.Default.Replay10,
                    contentDescription = Strings["player_seek_back"],
                    tint = colors.textMuted,
                    modifier = Modifier.size(32.dp)
                )
            }

            FilledIconButton(
                onClick = { state.togglePlayPause() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) Strings["player_pause"] else Strings["player_play"],
                    tint = if (state.isPlaying) colors.textPrimary else colors.textPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { state.seekForward() }) {
                Icon(
                    Icons.Default.Forward10,
                    contentDescription = Strings["player_seek_forward"],
                    tint = colors.textMuted,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { state.playNext() }) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = Strings["player_next"],
                    tint = colors.textMuted,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpeedSelector(
                currentSpeed = state.playbackSpeed,
                onSpeedSelected = { state.changePlaybackSpeed(it) }
            )

            SleepTimerButton(
                state = state,
                onClick = { showSleepTimer = true }
            )

            VolumeControl(
                currentVolume = state.volume,
                onVolumeChange = { state.changeVolume(it) },
                onToggleMute = { state.toggleMute() }
            )
        }
    }

    if (showQueue) {
        QueueDrawer(
            state = state,
            onDismiss = { showQueue = false }
        )
    }

    if (showSleepTimer) {
        SleepTimerSheet(
            state = state,
            onDismiss = { showSleepTimer = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueDrawer(
    state: MediaPlayerState,
    onDismiss: () -> Unit,
    onDownload: ((QueueItem) -> Unit)? = null,
    onDeleteDownload: ((QueueItem) -> Unit)? = null
) {
    val colors = PodiumTheme.colors
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // Panel
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(320.dp)
                .align(Alignment.CenterEnd),
            color = colors.surface
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
                // ── Header: "Up Next" + Clear ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Up Next",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            state.removeSelectedFromQueue(state.queue.indices.toSet())
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Clear",
                            color = colors.accent,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Queue list ──
                if (state.queue.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Strings["player_queue_empty"],
                            color = colors.textMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(state.queue) { index, item ->
                            val isActive = index == state.queueIndex
                            val bgColor = if (isActive) colors.elevated else Color.Transparent

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .background(bgColor)
                                    .clickable {
                                        if (isSelectionMode) {
                                            selectedIndices = if (index in selectedIndices) selectedIndices - index
                                            else selectedIndices + index
                                        } else {
                                            state.playFromQueue(index)
                                            onDismiss()
                                        }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Cover
                                Box {
                                    AsyncImage(
                                        model = item.artworkUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    // Active indicator: X button overlay
                                    if (isActive && !isSelectionMode) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(20.dp)
                                                .background(colors.surface.copy(alpha = 0.8f), CircleShape)
                                                .clickable { state.removeFromQueue(index) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = Strings["player_remove"],
                                                tint = colors.textPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    // Selection checkbox
                                    if (isSelectionMode) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(20.dp)
                                                .background(colors.surface, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Checkbox(
                                                checked = index in selectedIndices,
                                                onCheckedChange = { checked ->
                                                    selectedIndices = if (checked) selectedIndices + index
                                                    else selectedIndices - index
                                                },
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Title + Podcast
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = if (isActive) colors.accent else colors.textPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.episodeId != null) {
                                        Text(
                                            text = "",
                                            color = colors.textMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Duration placeholder or drag handle
                                if (!isSelectionMode) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Selection actions ──
                if (isSelectionMode && selectedIndices.isNotEmpty()) {
                    HorizontalDivider(color = colors.divider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onDownload != null) {
                            TextButton(onClick = {
                                selectedIndices.forEach { idx ->
                                    if (idx in state.queue.indices) onDownload(state.queue[idx])
                                }
                                selectedIndices = setOf()
                                isSelectionMode = false
                            }) {
                                Text(Strings["episode_download"], color = colors.textSecondary)
                            }
                        }
                        TextButton(onClick = {
                            state.removeSelectedFromQueue(selectedIndices)
                            selectedIndices = setOf()
                            isSelectionMode = false
                        }) {
                            Text(Strings["player_delete"], color = colors.danger)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    state: MediaPlayerState,
    onDismiss: () -> Unit
) {
    val options = listOf(5, 10, 15, 20, 30, 45, 60, 90)

    val sleepMinutes = state.sleepTimerMinutes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(Strings["player_sleep_timer"])
                if (sleepMinutes != null) {
                    Text(
                        text = Strings.get("player_sleep_timer_active", sleepMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column {
                if (state.sleepTimerMinutes != null) {
                    TextButton(
                        onClick = {
                            state.cancelSleepTimer()
                            onDismiss()
                        }
                    ) {
                        Text(Strings["player_cancel_timer"])
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                options.forEach { minutes ->
                    TextButton(
                        onClick = {
                            state.setSleepTimer(minutes)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.get("player_minutes", minutes))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings["dialog_close"])
            }
        }
    )
}

@Composable
private fun SleepTimerButton(
    state: MediaPlayerState,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Icon(
            Icons.Default.Timer,
            contentDescription = Strings["player_sleep_timer"],
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (state.sleepTimerMinutes != null) "${state.sleepTimerMinutes}m" else Strings["player_timer"]
        )
    }
}

@Composable
private fun SpeedSelector(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("${currentSpeed}x")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                DropdownMenuItem(
                    text = { Text("${speed}x") },
                    onClick = {
                        onSpeedSelected(speed)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun VolumeControl(
    currentVolume: Int,
    onVolumeChange: (Int) -> Unit,
    onToggleMute: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            if (currentVolume > 0) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = if (currentVolume > 0) Strings["player_mute"] else Strings["player_unmute"],
            modifier = Modifier.size(20.dp).clickable { onToggleMute() }
        )
        Slider(
            value = currentVolume.toFloat() / 100f,
            onValueChange = { onVolumeChange((it * 100).toInt()) },
            modifier = Modifier.width(100.dp)
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
