package app.podiumpodcasts.podium.sqldelight.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<PodiumDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = PodiumDatabaseImpl.Schema

internal fun KClass<PodiumDatabase>.newInstance(driver: SqlDriver): PodiumDatabase =
    PodiumDatabaseImpl(driver)

private class PodiumDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver),
    PodiumDatabase {
  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> = QueryResult.Unit

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
