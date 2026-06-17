package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.repository.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit
) {
    val enableArtworkColors by settingsRepository.appearance.enableArtworkColors.collectAsState()
    val playerPlaybackSpeed by settingsRepository.behavior.playerPlaybackSpeed.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Enable artwork colors")
                Switch(
                    checked = enableArtworkColors,
                    onCheckedChange = {
                        scope.launch { settingsRepository.appearance.setEnableArtworkColors(it) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Playback", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Playback speed: ${playerPlaybackSpeed}x")

            Spacer(modifier = Modifier.height(16.dp))
            Text("About", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Podium - Podcast Player", style = MaterialTheme.typography.bodyMedium)
            Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall)
        }
    }
}
