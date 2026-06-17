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
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "app.podiumpodcasts.podium.desktop.MainKt"

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
