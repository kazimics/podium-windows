package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger
import java.io.File

private const val TAG = "MpvNativeLoader"

object MpvNativeLoader {
    private var loaded = false

    fun load(): Boolean {
        if (loaded) return true
        synchronized(this) {
            if (loaded) return true
            try {
                val nativeDir = File(System.getProperty("user.home"), ".podium/native")
                nativeDir.mkdirs()

                val dlls = listOf("mpv-1.dll")

                val baseDir = File(System.getProperty("user.dir"))
                val libDirs = listOf(
                    File(baseDir, "libs"),
                    File(baseDir, "../libs"),
                    File(baseDir, "../../libs")
                )

                for (dllName in dlls) {
                    val outFile = File(nativeDir, dllName)
                    if (!outFile.exists()) {
                        var copied = false
                        for (libDir in libDirs) {
                            val srcFile = File(libDir, dllName)
                            if (srcFile.exists()) {
                                srcFile.copyTo(outFile, overwrite = true)
                                Logger.d(TAG, "Copied: $dllName (${outFile.length()} bytes)")
                                copied = true
                                break
                            }
                        }
                        if (!copied) {
                            Logger.e(TAG, "DLL not found: $dllName")
                            return false
                        }
                    }
                }

                for (dllName in dlls) {
                    val dll = File(nativeDir, dllName)
                    if (dll.exists()) {
                        try {
                            System.load(dll.absolutePath)
                            Logger.d(TAG, "Loaded: $dllName")
                        } catch (e: UnsatisfiedLinkError) {
                            Logger.e(TAG, "Cannot load $dllName: ${e.message}")
                            return false
                        }
                    }
                }

                loaded = true
                Logger.i(TAG, "mpv native library ready")
                return true
            } catch (e: Throwable) {
                Logger.e(TAG, "Failed to load mpv native library", e)
                return false
            }
        }
    }
}
