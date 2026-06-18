package app.podiumpodcasts.podium.utils

import java.io.File
import java.util.Properties

object Settings {
    private val settingsFile = File(System.getProperty("user.home"), ".podium/settings.properties")
    private val props = Properties()

    init {
        try {
            if (settingsFile.exists()) {
                settingsFile.inputStream().use { props.load(it) }
            }
        } catch (e: Exception) {
            Logger.e("Settings", "Failed to load settings", e)
        }
    }

    fun getDownloadPath(): String {
        return props.getProperty("download_path", defaultDownloadPath())
    }

    fun setDownloadPath(path: String) {
        props.setProperty("download_path", path)
        save()
    }

    fun resetDownloadPath() {
        props.remove("download_path")
        save()
    }

    private fun save() {
        try {
            settingsFile.parentFile?.mkdirs()
            settingsFile.outputStream().use { props.store(it, "Podium Settings") }
        } catch (e: Exception) {
            Logger.e("Settings", "Failed to save settings", e)
        }
    }

    private fun defaultDownloadPath(): String {
        return File(System.getProperty("user.home"), ".podium/downloads").absolutePath
    }
}
