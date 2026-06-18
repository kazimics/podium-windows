import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)

                implementation(project(":shared"))

                implementation(libs.kotlinx.coroutines.core)

                // JDBC SQLite for desktop
                implementation("org.xerial:sqlite-jdbc:3.46.1.0")

                // JLayer for MP3 audio playback
                implementation(files("../libs/jlayer.jar"))
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.compose.ui:ui-test:1.9.0")
                implementation("org.jetbrains.compose.ui:ui-test-junit4:1.9.0")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "app.podiumpodcasts.podium.desktop.MainKt"

        jvmArgs += "-Dfile.encoding=UTF-8"
        jvmArgs += "--add-modules=java.sql"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "podium"
            packageVersion = "0.1.0"
            description = "Podium Podcasts"
            vendor = "Podium Podcasts"

            windows {
                menuGroup = "Podium Podcasts"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                dirChooser = true
                shortcut = true
                menu = true
            }
        }
    }
}

tasks.matching { it.name == "createRuntimeImage" }.configureEach {
    doLast {
        try {
            // Access the jlink command arguments via reflection
            val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
            val jmodsPath = "$javaHome/jmods"

            // Find the runtime output directory
            val runtimeDir = File(project.buildDir, "compose/tmp/main/runtime")

            println("Adding java.sql module to JRE at ${runtimeDir.absolutePath}")

            // Run jlink to add java.sql to the existing modules
            val tempDir = File(runtimeDir.parentFile, "runtime_with_sql")
            if (tempDir.exists()) tempDir.deleteRecursively()

            val process = ProcessBuilder(
                File(javaHome, "bin/jlink").absolutePath,
                "--module-path", jmodsPath,
                "--add-modules", "ALL-MODULE-PATH,java.sql",
                "--output", tempDir.absolutePath,
                "--no-header-files",
                "--no-man-pages",
                "--strip-debug"
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0 && tempDir.exists()) {
                runtimeDir.deleteRecursively()
                tempDir.renameTo(runtimeDir)
                println("JRE patched successfully with java.sql module")
            } else {
                println("jlink warning (exit code $exitCode): $output")
            }
        } catch (e: Exception) {
            println("Warning: Could not patch JRE with java.sql: ${e.message}")
        }
    }
}
