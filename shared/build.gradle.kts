plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json.core)

                // Ktor
                implementation(libs.ktor.client.core.kmp)
                implementation(libs.ktor.client.content.negotiation.kmp)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.client.logging.kmp)

                // RSS
                implementation(libs.rssparser.kmp)

                // Settings
                implementation(libs.multiplatform.settings.core)
                implementation(libs.multiplatform.settings.coroutines)

                // SQLDelight
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

sqldelight {
    databases {
        create("PodiumDatabase") {
            packageName.set("app.podiumpodcasts.podium.sqldelight")
            verifyMigrations.set(false)
            deriveSchemaFromMigrations.set(true)
        }
    }
}
