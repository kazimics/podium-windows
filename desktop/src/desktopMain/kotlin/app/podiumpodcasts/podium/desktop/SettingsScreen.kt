package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.manager.ExportManager
import app.podiumpodcasts.podium.manager.ImportManager
import app.podiumpodcasts.podium.manager.ImportResult
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    database: AppDatabase,
    onBack: () -> Unit
) {
    val exportManager = remember { ExportManager(database) }
    val importManager = remember { ImportManager(database) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedOpml by remember { mutableStateOf<String?>(null) }
    var showCopiedSnackbar by remember { mutableStateOf(false) }
    var showImportResult by remember { mutableStateOf<ImportResult?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Text("Data", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Export OPML") },
                supportingContent = { Text("Export your podcast subscriptions as OPML") },
                leadingContent = { Icon(Icons.Default.FileDownload, null) },
                trailingContent = {
                    TextButton(onClick = {
                        scope.launch {
                            exportedOpml = exportManager.exportOpml()
                            showExportDialog = true
                        }
                    }) {
                        Text("Export")
                    }
                }
            )

            ListItem(
                headlineContent = { Text("Import OPML") },
                supportingContent = { Text("Import podcast subscriptions from OPML file") },
                leadingContent = { Icon(Icons.Default.FileUpload, null) },
                trailingContent = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                isImporting = true
                                try {
                                    val file = openFilePicker("Select OPML File", "opml", "xml")
                                    if (file != null) {
                                        val content = file.readText()
                                        val result = importManager.importOpml(content)
                                        showImportResult = result
                                    }
                                } finally {
                                    isImporting = false
                                }
                            }
                        },
                        enabled = !isImporting
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Import")
                        }
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("About", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Podium - Podcast Player", style = MaterialTheme.typography.bodyMedium)
            Text("Version 0.1.0", style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showExportDialog && exportedOpml != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export OPML") },
            text = {
                Column {
                    Text("Copy the OPML content below:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedOpml!!,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 400.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(exportedOpml!!))
                    showExportDialog = false
                    showCopiedSnackbar = true
                }) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showCopiedSnackbar) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showCopiedSnackbar = false }) {
                    Text("OK")
                }
            }
        ) {
            Text("OPML copied to clipboard!")
        }
    }

    showImportResult?.let { result ->
        when (result) {
            is ImportResult.Success -> {
                AlertDialog(
                    onDismissRequest = { showImportResult = null },
                    title = { Text("Import Complete") },
                    text = {
                        Column {
                            Text("Added: ${result.added} podcasts")
                            Text("Skipped (duplicates): ${result.skipped}")
                            if (result.failed > 0) {
                                Text("Failed: ${result.failed}", color = MaterialTheme.colorScheme.error)
                                result.errors.forEach { error ->
                                    Text(
                                        text = "  - $error",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showImportResult = null }) {
                            Text("OK")
                        }
                    }
                )
            }
            is ImportResult.Error -> {
                AlertDialog(
                    onDismissRequest = { showImportResult = null },
                    title = { Text("Import Failed") },
                    text = { Text(result.message) },
                    confirmButton = {
                        TextButton(onClick = { showImportResult = null }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

private fun openFilePicker(title: String, vararg extensions: String): File? {
    val frame = Frame()
    val dialog = FileDialog(frame, title, FileDialog.LOAD)
    dialog.file = "*.${extensions.first()}"
    dialog.isVisible = true

    val fileName = dialog.file
    val dir = dialog.directory
    dialog.dispose()
    frame.dispose()

    return if (fileName != null && dir != null) {
        File(dir, fileName)
    } else {
        null
    }
}
