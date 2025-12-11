package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.local.dao.TorznabConfigDao
import com.prajwalch.torrentsearch.data.local.entities.TorznabConfigEntity
import com.prajwalch.torrentsearch.data.local.entities.toSearchProviderInfo
import com.prajwalch.torrentsearch.data.local.entities.toTorznabConfig
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.providers.AnimeTosho
import com.prajwalch.torrentsearch.providers.BitSearch
import com.prajwalch.torrentsearch.providers.Eztv
import com.prajwalch.torrentsearch.providers.Knaben
import com.prajwalch.torrentsearch.providers.LimeTorrents
import com.prajwalch.torrentsearch.providers.MyPornClub
import com.prajwalch.torrentsearch.providers.Nyaa
import com.prajwalch.torrentsearch.providers.SearchProvider
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderInfo
import com.prajwalch.torrentsearch.providers.Sukebei
import com.prajwalch.torrentsearch.providers.ThePirateBay
import com.prajwalch.torrentsearch.providers.TheRarBg
import com.prajwalch.torrentsearch.providers.TokyoToshokan
import com.prajwalch.torrentsearch.providers.TorrentDownloads
import com.prajwalch.torrentsearch.providers.TorrentsCSV
import com.prajwalch.torrentsearch.providers.TorznabConfig
import com.prajwalch.torrentsearch.providers.TorznabSearchProvider
import com.prajwalch.torrentsearch.providers.UIndex
import com.prajwalch.torrentsearch.providers.XXXClub
import com.prajwalch.torrentsearch.providers.Yts
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SearchProvidersRepository @Inject constructor(
    private val torznabConfigDao: TorznabConfigDao,
    private val httpClient: HttpClient,
) {

    private val builtins = listOf(
        AnimeTosho(),
        BitSearch(),
        Eztv(),
        Knaben(),
        LimeTorrents(),
        MyPornClub(),
        Nyaa(),
        Sukebei(),
        ThePirateBay(),
        TheRarBg(),
        TokyoToshokan(),
        TorrentDownloads(),
        TorrentsCSV(),
        UIndex(),
        XXXClub(),
        Yts(),
    )

    // TODO: Remove this or handle enabled by default search providers properly.
    fun getEnabledSearchProvidersId(): Set<SearchProviderId> {
        return builtins.filter { it.info.enabledByDefault }.map { it.info.id }.toSet()
    }

    fun observeSearchProvidersInfo(): Flow<List<SearchProviderInfo>> {
        val builtinSearchProvidersInfoFlow = flowOf(builtins.map { it.info })
        val torznabSearchProvidersInfoFlow = torznabConfigDao.observeAll().map {
            it.toSearchProviderInfo()
        }

        return combine(
            builtinSearchProvidersInfoFlow,
            torznabSearchProvidersInfoFlow,
        ) { builtinInfos, torznabInfos ->
            builtinInfos + torznabInfos
        }
    }

    fun observeSearchProvidersCount(): Flow<Int> {
        return torznabConfigDao.observeCount().map { it + builtins.size }
    }

    suspend fun getSearchProvidersInstance(category: Category): List<SearchProvider> {
        val searchProviders = getSearchProvidersInstance()

        if (category == Category.All) {
            return searchProviders
        }

        return searchProviders.filter {
            // NOTE: see original comment in your code.
            (it.info.specializedCategory == Category.All) ||
                (category == it.info.specializedCategory)
        }
    }

    suspend fun getSearchProvidersInstance(): List<SearchProvider> {
        val builtinSearchProvidersFlow = flowOf(builtins)
        val torznabSearchProvidersFlow = torznabConfigDao.observeAll().map { entities ->
            entities.map { TorznabSearchProvider(config = it.toTorznabConfig()) }
        }

        return combine(
            builtinSearchProvidersFlow,
            torznabSearchProvidersFlow,
        ) { builtinProviders, externalProviders ->
            builtinProviders + externalProviders
        }.firstOrNull().orEmpty()
    }

    suspend fun addTorznabConfig(
        name: String,
        url: String,
        apiKey: String,
        category: Category,
    ) {
        val configEntity = TorznabConfigEntity(
            name = name,
            url = url.trimEnd { it == '/' },
            apiKey = apiKey,
            category = category.name,
        )
        torznabConfigDao.insert(entity = configEntity)
    }

    /**
     * Synchronise Torznab providers from a Jackett instance.
     *
     * This function expects the Jackett base URL (for example: http://192.168.1.175:9117)
     * and the Jackett API key. It will query Jackett's indexers list and
     * add or update Torznab configs for each indexer so they show up in the app.
     */
    suspend fun syncFromJackett(baseUrl: String, apiKey: String) {
        // Normalize base url (no trailing slash)
        val base = baseUrl.trimEnd { it == '/' }

        // Jackett indexers list endpoint (v2)
        val indexersUrl = "$base/api/v2.0/indexers"

        val json = httpClient.withExceptionHandler {
            httpClient.getJson("$indexersUrl?apikey=$apiKey")
        }

        when (json) {
            is com.prajwalch.torrentsearch.network.HttpClientResponse.Ok<*> -> {
                val element = json.result as? JsonElement ?: return
                val array = element.jsonArray

                for (item in array) {
                    val obj: JsonObject = item.jsonObject

                    // Try a few fields commonly present in Jackett responses
                    val title = obj["Title"]?.jsonPrimitive?.safeContentOrNull()
                        ?: obj["title"]?.jsonPrimitive?.safeContentOrNull()
                        ?: obj["Name"]?.jsonPrimitive?.safeContentOrNull()
                        ?: obj["name"]?.jsonPrimitive?.safeContentOrNull()
                        ?: "Unknown"

                    // Jackett exposes an "Id" or "IndexerId" which we can use
                    val id = obj["Id"]?.jsonPrimitive?.safeContentOrNull()
                        ?: obj["IndexerId"]?.jsonPrimitive?.safeContentOrNull()
                        ?: obj["id"]?.jsonPrimitive?.safeContentOrNull()

                    // If no id available, try to see if the object contains a 'Config' with a 'Url' we can reuse
                    val torznabPath = if (id != null) {
                        // Construct Torznab path for the indexer
                        "$base/api/v2.0/indexers/$id/results/torznab"
                    } else {
                        // Fallback: try to use any URL present in the config
                        val configUrl = obj["Config"]
                            ?.jsonObject
                            ?.get("Url")
                            ?.jsonPrimitive
                            ?.safeContentOrNull()
                        configUrl ?: continue
                    }

                    // Check if config already exists for this URL
                    val existing = torznabConfigDao.findByUrl(torznabPath)

                    if (existing != null) {
                        // Update existing entry with latest name/apiKey
                        torznabConfigDao.update(
                            existing.copy(
                                name = title,
                                url = torznabPath,
                                apiKey = apiKey,
                            ),
                        )
                    } else {
                        // Insert new config
                        val entity = TorznabConfigEntity(
                            name = title,
                            url = torznabPath,
                            apiKey = apiKey,
                            category = Category.All.name,
                        )
                        torznabConfigDao.insert(entity = entity)
                    }
                }
            }
            else -> {
                // Quietly ignore network errors here — caller can show a message if needed
                return
            }
        }
    }

    /**
     * Finds all torznab configs labelled as a Jackett master config (name == "Jackett")
     * and performs sync for each. This allows the UI to store a single Jackett entry
     * and let the app pull all indexers from it.
     */
    suspend fun syncAllJackettConfigs() {
        val entities = torznabConfigDao.observeAll().first()
        val jackettEntries = entities.filter { it.name.equals("Jackett", ignoreCase = true) }
        for (entry in jackettEntries) {
            try {
                syncFromJackett(baseUrl = entry.url, apiKey = entry.apiKey)
            } catch (_: Exception) {
                // continue on errors
            }
        }
    }

    suspend fun findTorznabConfig(id: SearchProviderId): TorznabConfig? {
        return torznabConfigDao.findById(id = id)?.toTorznabConfig()
    }

    suspend fun updateTorznabConfig(
        id: SearchProviderId,
        name: String,
        url: String,
        apiKey: String,
        category: Category,
    ) {
        val configEntity = TorznabConfigEntity(
            id = id,
            name = name,
            url = url,
            apiKey = apiKey,
            category = category.name,
        )
        torznabConfigDao.update(entity = configEntity)
    }

    suspend fun deleteTorznabConfig(id: SearchProviderId) {
        torznabConfigDao.deleteById(id = id)
    }

    // Local helper to safely get string content without throwing
    private fun JsonPrimitive.safeContentOrNull(): String? = try {
        this.content
    } catch (_: Exception) {
        null
    }
}
