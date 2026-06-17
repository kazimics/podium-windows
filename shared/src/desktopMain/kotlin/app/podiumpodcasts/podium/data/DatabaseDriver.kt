package app.podiumpodcasts.podium.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase
import java.io.File

actual fun createDatabaseDriver(databaseFile: File): SqlDriver {
    val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
    if (!databaseFile.exists()) {
        PodiumDatabase.Schema.create(driver)
    }
    return driver
}
