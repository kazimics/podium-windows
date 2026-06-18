package app.podiumpodcasts.podium.desktop

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

fun main() {
    val logFile = File(System.getProperty("user.home"), ".podium/crash.log")

    try {
        application {
            val windowState = rememberWindowState(
                size = DpSize(1200.dp, 800.dp),
                position = WindowPosition(Alignment.Center)
            )

            Window(
                onCloseRequest = ::exitApplication,
                state = windowState,
                title = "Podium"
            ) {
                App()
            }
        }
    } catch (e: Exception) {
        logFile.parentFile?.mkdirs()
        logFile.appendText(
            "[${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}] FATAL: ${e.message}\n${e.stackTraceToString()}\n\n"
        )
        throw e
    }
}
