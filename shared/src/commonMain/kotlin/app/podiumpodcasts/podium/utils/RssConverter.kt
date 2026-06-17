package app.podiumpodcasts.podium.utils

import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import java.security.MessageDigest

object RssConverter {

    fun toPodcast(channel: RssChannel, origin: String, fileSize: Long, seedColor: Int?): Podcast {
        return Podcast(
            origin = origin,
            link = channel.link ?: "",
            title = channel.title ?: "",
            description = channel.description ?: "",
            author = channel.ownerName ?: "",
            imageUrl = channel.image?.url ?: "",
            imageSeedColor = seedColor ?: 0,
            languageCode = channel.language ?: "",
            fileSize = fileSize
        )
    }

    fun toPodcastEpisode(item: RssItem, podcast: Podcast): PodcastEpisode {
        val guid = item.guid ?: item.link ?: item.title ?: ""
        val id = "${podcast.origin}:${guid}"

        return PodcastEpisode(
            id = id,
            guid = guid,
            origin = podcast.origin,
            link = item.link ?: "",
            title = item.title ?: "",
            description = item.description ?: "",
            imageUrl = item.image,
            author = item.author ?: podcast.author,
            pubDate = parseDate(item.pubDate),
            duration = parseDuration(item.itunesDuration),
            audioUrl = item.audio ?: "",
            podcastTitle = podcast.title,
            imageSeedColor = podcast.imageSeedColor
        )
    }

    private fun parseDate(dateString: String?): Long {
        if (dateString == null) return 0
        return try {
            val sdf = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US)
            sdf.parse(dateString)?.time ?: 0
        } catch (e: Exception) {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                    .parse(dateString)?.time ?: 0
            } catch (e2: Exception) {
                0
            }
        }
    }

    private fun parseDuration(durationString: String?): Int {
        if (durationString == null) return 0
        return try {
            val parts = durationString.split(":").map { it.trim().toIntOrNull() ?: 0 }
            when (parts.size) {
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                2 -> parts[0] * 60 + parts[1]
                1 -> parts[0]
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }
}

fun String.sha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
