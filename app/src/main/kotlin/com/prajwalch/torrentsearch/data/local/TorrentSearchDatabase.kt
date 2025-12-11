package com.prajwalch.torrentsearch.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RenameTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.prajwalch.torrentsearch.data.local.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.local.dao.SearchHistoryDao
import com.prajwalch.torrentsearch.data.local.dao.TorznabConfigDao
import com.prajwalch.torrentsearch.data.local.entities.BookmarkedTorrent
import com.prajwalch.torrentsearch.data.local.entities.SearchHistoryEntity
import com.prajwalch.torrentsearch.data.local.entities.TorznabConfigEntity
import com.prajwalch.torrentsearch.models.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Application database. */
@Database(
    entities = [
        BookmarkedTorrent::class,
        SearchHistoryEntity::class,
        TorznabConfigEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(
            from = 2,
            to = 3,
            spec = TorrentSearchDatabase.Migration2To3Spec::class,
        ),
    ],
)
abstract class TorrentSearchDatabase : RoomDatabase() {

    abstract fun bookmarkedTorrentDao(): BookmarkedTorrentDao

    abstract fun searchHistoryDao(): SearchHistoryDao

    abstract fun torznabConfigDao(): TorznabConfigDao

    @DeleteColumn(tableName = "torznab_search_providers", columnName = "unsafeReason")
    @RenameTable(fromTableName = "torznab_search_providers", toTableName = "torznab_configs")
    class Migration2To3Spec : AutoMigrationSpec

    companion object {
        /** Name of the database file. */
        private const val DB_NAME = "torrentsearch.db"

        /**
         * Single instance of the database.
         *
         * Recommended to re-use the reference once database is created.
         */
        private var Instance: TorrentSearchDatabase? = null

        /** Returns the instance of the database. */
        fun getInstance(context: Context): TorrentSearchDatabase {
            return Instance ?: createInstance(context = context)
        }

        /** Creates, stores and returns the instance of the database. */
        private fun createInstance(context: Context): TorrentSearchDatabase {
            val databaseBuilder = Room.databaseBuilder(
                context = context,
                klass = TorrentSearchDatabase::class.java,
                name = DB_NAME,
            )

            val db = databaseBuilder.build().also { Instance = it }

            // Pre-seed a Jackett master Torznab config if the DB is empty.
            // This inserts a single "Jackett" entry pointing to the user's
            // local Jackett server. The sync button will expand this into
            // per-indexer Torznab configs by querying the Jackett API.
            try {
                // Use an explicit IO coroutine scope instead of raw GlobalScope
                val ioScope = CoroutineScope(Dispatchers.IO)
                ioScope.launch {
                    val dao = db.torznabConfigDao()
                    val count = dao.observeCount().first()
                    if (count == 0) {
                        val jackettApiKey = "sfbizvj42r5h41a2aojb2t29zougqd3s"
                        // NOTE: trailing slash is often safer for Torznab; adjust if needed elsewhere.
                        val jackettBaseUrl = "http://192.168.1.175:9117/"

                        val entity = TorznabConfigEntity(
                            name = "Jackett",
                            url = jackettBaseUrl,
                            apiKey = jackettApiKey,
                            category = Category.All.name,
                        )
                        dao.insert(entity = entity)
                    }
                }
            } catch (_: Exception) {
                // Do not crash if pre-seed fails.
            }

            return db
        }
    }
}
