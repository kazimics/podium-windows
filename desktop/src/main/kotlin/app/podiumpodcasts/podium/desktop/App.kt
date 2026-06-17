package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.DatabaseManager
import app.podiumpodcasts.podium.data.repository.SettingsRepository
import app.podiumpodcasts.podium.ui.route.*
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import com.russhwolf.settings.PreferencesSettings
import java.io.File

@Composable
fun App() {
    val databaseDir = remember {
        val userHome = System.getProperty("user.home")
        File(userHome, ".podium")
    }

    val database = remember { DatabaseManager.build(databaseDir) }
    val settingsRepository = remember {
        val prefs = java.util.prefs.Preferences.userRoot().node("podium")
        SettingsRepository(PreferencesSettings(prefs))
    }

    var currentRoute by remember { mutableStateOf<Any>(Home) }
    var currentEpisodeId by remember { mutableStateOf<String?>(null) }
    var currentOrigin by remember { mutableStateOf<String?>(null) }

    PodiumTheme {
        when (val route = currentRoute) {
            is Home -> HomeRoute(
                podcastRepository = database.podcasts,
                onClickPodcast = { origin ->
                    currentOrigin = origin
                    currentRoute = PodcastDetail(origin)
                },
                onClickSettings = {
                    currentRoute = Settings
                }
            )

            is PodcastDetail -> PodcastDetailRoute(
                origin = route.origin,
                podcastRepository = database.podcasts,
                episodeRepository = database.episodes,
                onClickEpisode = { episodeId ->
                    currentEpisodeId = episodeId
                    currentRoute = EpisodeDetail(episodeId)
                },
                onBack = {
                    currentRoute = Home
                }
            )

            is EpisodeDetail -> EpisodeDetailRoute(
                episodeId = route.episodeId,
                episodeRepository = database.episodes,
                onPlay = { episodeId ->
                    // TODO: Play episode with vlcj
                },
                onBack = {
                    currentOrigin?.let {
                        currentRoute = PodcastDetail(it)
                    } ?: run {
                        currentRoute = Home
                    }
                }
            )

            is Discover -> DiscoverRoute(
                onClickPodcast = { origin ->
                    currentOrigin = origin
                    currentRoute = PodcastDetail(origin)
                },
                onBack = {
                    currentRoute = Home
                }
            )

            is Settings -> SettingsRoute(
                settingsRepository = settingsRepository,
                onBack = {
                    currentRoute = Home
                }
            )

            else -> {
                // Unknown route, go to home
                currentRoute = Home
            }
        }
    }
}
