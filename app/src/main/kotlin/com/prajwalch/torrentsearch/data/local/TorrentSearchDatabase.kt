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

    @DeleteColumn(
        tableName = "torznab_search_providers",
        columnName = "unsafeReason",
    )
    @RenameTable(
        fromTableName = "torznab_search_providers",
        toTableName = "torznab_configs",
    )
    class Migration2To3Spec : AutoMigrationSpec

    companion object {
        private const val DB_NAME = "torrentsearch.db"

        @Volatile
        private var instance: TorrentSearchDatabase? = null

        fun getInstance(context: Context): TorrentSearchDatabase {
            val current = instance
            if (current != null) return current

            return createInstance(context).also { created ->
                instance = created
            }
        }

        private fun createInstance(context: Context): TorrentSearchDatabase {
            val builder = Room.databaseBuilder(
                context,
                TorrentSearchDatabase::class.java,
                DB_NAME,
            )

            val db = builder.build()

            // Store reference for future calls.
            instance = db

            // When the database is first created and contains no Torznab configs,
            // insert a single "Jackett" entry pointing at the user's local
            // Jackett instance running on localhost. The app's sync logic will
            // expand this into per-indexer configs by querying the Jackett API.
            try {
                val ioScope = CoroutineScope(Dispatchers.IO)
                ioScope.launch {
                    val configDao = db.torznabConfigDao()
                    val existingCount = configDao.observeCount().first()

                    if (existingCount == 0) {
                        val jackettApiKey = "sfbizvj42r5h41a2aojb2t29zougqd3s"
                        val jackettBaseUrl = "http://localhost:9117/"

                        val initialConfig = TorznabConfigEntity(
                            name = "Jackett",
                            url = jackettBaseUrl,
                            apiKey = jackettApiKey,
                            category = Category.All.name,
                        )

                        configDao.insert(initialConfig)
                    }
                }
            } catch (_: Exception) {
                // Swallow any errors here, since failure to pre-seed
                // should not prevent the app from starting.
            }

            return db
        }
    }
}
