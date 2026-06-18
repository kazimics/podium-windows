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

                // vlcj media player
                implementation(libs.vlcj.core)
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

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "podium"
            packageVersion = "1.0.0"
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

tasks.register("patchRuntimeModules") {
    dependsOn("createRuntimeImage")
    doLast {
        val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
        val jmodsDir = File(javaHome, "jmods")
        val javaSqlJmod = File(jmodsDir, "java.sql.jmod")

        val runtimeDirs = File(project.buildDir, "compose").walkTopDown()
            .filter { it.name == "modules" && it.parentFile?.name == "lib" }
            .map { it.parentFile.parentFile }
            .toList()

        for (runtimeDir in runtimeDirs) {
            if (javaSqlJmod.exists()) {
                val modulesFile = File(runtimeDir, "lib/modules")
                val existingModules = if (modulesFile.exists()) {
                    modulesFile.readText().trim().split("\n").filter { it.isNotBlank() }
                } else emptyList()

                if ("java.sql" in existingModules) {
                    println("java.sql already in JRE, skipping patch")
                    continue
                }

                val allModules = (existingModules + "java.sql").joinToString(",")
                println("Patching JRE at ${runtimeDir.absolutePath} with java.sql module")
                val tempDir = File(runtimeDir.parentFile, "runtime_patched")
                if (tempDir.exists()) tempDir.deleteRecursively()
                val jlinkPath = File(javaHome, "bin/jlink").absolutePath
                val process = ProcessBuilder(
                    jlinkPath,
                    "--module-path", jmodsDir.absolutePath,
                    "--add-modules", allModules,
                    "--output", tempDir.absolutePath,
                    "--no-header-files",
                    "--no-man-pages",
                    "--strip-debug"
                ).redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("jlink failed with exit code $exitCode:\n$output")
                }
                runtimeDir.deleteRecursively()
                tempDir.renameTo(runtimeDir)
            }
        }
    }
}

tasks.matching { it.name == "packageMsi" || it.name == "packageExe" }.configureEach {
    dependsOn("patchRuntimeModules")
}
