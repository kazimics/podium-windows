package app.podiumpodcasts.podium.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.data.model.PodcastEpisodeBundle
import coil3.compose.AsyncImage

@Composable
fun EpisodeListItem(
    bundle: PodcastEpisodeBundle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val episode = bundle.episode
    val isPlayed = bundle.playState?.played == true
    val isNew = episode.isNew

    ListItem(
        headlineContent = {
            Text(
                text = episode.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = episode.podcastTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (episode.duration > 0) {
                    Text(
                        text = formatDuration(episode.duration),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        leadingContent = {
            AsyncImage(
                model = episode.imageUrl,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        },
        trailingContent = {
            if (isNew) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("NEW") }
                )
            }
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, secs)
        else -> "%d:%02d".format(minutes, secs)
    }
}
