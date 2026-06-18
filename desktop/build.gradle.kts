import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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

tasks.matching { it.name == "createRuntimeImage" }.configureEach {
    doFirst {
        val addModulesArg = "--add-modules=java.sql"
        val jvmArgsField = this.javaClass.superclass?.getDeclaredField("jvmArgs")
            ?: this.javaClass.getDeclaredField("jvmArgs")
        jvmArgsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val jvmArgsList = jvmArgsField.get(this) as MutableList<String>
        if (addModulesArg !in jvmArgsList) {
            jvmArgsList.add(addModulesArg)
        }
    }
}
