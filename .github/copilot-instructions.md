# AI Coding Agent Instructions for TorrentSearch

## Project Overview

TorrentSearch is a Material 3-designed Android app for searching torrents from multiple providers. It's built with **Kotlin**, **Jetpack Compose**, **Room Database**, and **Hilt Dependency Injection** (compileSdk 36, minSdk 25, version 0.4.4).

**Key Goals:**
- Multi-source torrent search with pluggable providers
- Local caching of bookmarks and search history  
- Safe mode with NSFW filtering
- External Torznab API integration (Jackett/Prowlarr)
- 12+ language localization via Weblate

---

## Architecture Overview

### Layered Architecture (Clean Architecture Pattern)

The codebase follows a strict separation of concerns:

```
UI (Jetpack Compose) 
  ↓ (observes)
ViewModels (Hilt-injected)
  ↓ (uses)
Use Cases (SearchTorrentsUseCase)
  ↓ (orchestrates)
Repositories (TorrentsRepository, SettingsRepository, etc.)
  ↓ (combines)
Remote Data Source (API/parsing) + Local Data Source (Room DB)
  ↓
Network Layer (Ktor HttpClient) + Database Layer (Room)
```

**Critical Files:**
- `ui/MainViewModel.kt` - App-level theme/settings state
- `ui/search/SearchViewModel.kt` - Search orchestration and result processing
- `usecases/SearchTorrentsUseCase.kt` - Business logic for search (filtering, result limits)
- `data/repository/TorrentsRepository.kt` - Coordinates remote & local data
- `data/remote/TorrentsRemoteDataSource.kt` - Parallel provider search
- `providers/SearchProvider.kt` - Interface for all torrent sources

### Plugin Architecture: Search Providers

**All torrent sources** implement `SearchProvider` interface:

```kotlin
interface SearchProvider {
    val info: SearchProviderInfo  // metadata: id, name, URL, category specialization, safety status
    suspend fun search(query: String, context: SearchContext): List<Torrent>
}
```

**Built-in providers** in `providers/`:
- `ThePirateBay.kt`, `LimeTorrents.kt`, `Yts.kt`, `Nyaa.kt` (anime), `Eztv.kt` (TV), etc.
- Each scrapes HTML/JSON with provider-specific parsing logic
- Safety status (`Safe` vs `Unsafe(reason)`) determines visibility based on settings

**External providers:**
- `Torznab.kt` - Generic adapter for Jackett/Prowlarr APIs
- Config stored in Room DB (`TorznabConfigEntity`)
- Added dynamically at runtime via `SearchProvidersRepository`

### Data Flow: Search Operation

1. User enters query in `SearchScreen` → `SearchViewModel` receives query & category from `SavedStateHandle`
2. `SearchViewModel.loadResults()` calls `SearchTorrentsUseCase(query, category)`
3. Use case fetches **enabled providers** + **result limit** from `SettingsRepository`
4. `TorrentsRepository.search()` delegates to `TorrentsRemoteDataSource.searchTorrents()`
5. Remote data source launches **parallel coroutines per provider** via `channelFlow { launch { ... } }`
6. Each provider search runs concurrently; results sent as `Result.success()` or `Result.failure(SearchProviderException)`
7. `TorrentsRepository` collects batches, accumulates successes/failures into `SearchResults`, emits incrementally
8. `SearchViewModel` receives updates via `onSearchResultsReceived()`, applies sorting/filtering/limits on the fly
9. UI derives state via `combine(internalState, enableNSFWMode)` → `uiState` Flow

**Critical Pattern:** Results emit **incrementally as providers complete**—not waiting for all. Results can be cancelled mid-search if limit reached (`transformWhile` in use case).

### State Management Pattern

- **ViewModels** use `MutableStateFlow` for internal mutable state (`InternalState` data class)
- Flow transforms applied via `combine()` to **derive read-only `UiState`** on demand
- `stateIn(scope=viewModelScope, started=SharingStarted.WhileSubscribed(5.seconds), initialValue=...)` caches derived state
- Example: `SearchViewModel.uiState` = `combine(internalState, enableNSFWMode) { internal, nsfw -> createUiState(...) }`
- UI subscribes via `collectAsStateWithLifecycle()` to cancel collection on pause

### Database Layer (Room)

Schema in `app/schemas/`:
- `BookmarkedTorrent` - user bookmarks (id: provider + torrent hash, for deduplication)
- `SearchHistoryEntity` - query history (id, query, timestamp)
- `TorznabConfigEntity` - external provider configs (id, name, URL, API key, auth, etc.)

**Migrations:** Version 3 current; auto-migrations 1→2, custom `Migration2To3Spec` for 2→3. DAOs emit `Flow<List<Entity>>` for reactive queries. Export schema JSON snapshots to `app/schemas/` for migration safety.

---

## Developer Workflows

### Building

```bash
# Debug build 
./gradlew assembleDebug

# Release build with ProGuard minification
./gradlew assembleRelease

# Install & run on emulator/device
./gradlew installDebug
adb shell am start -n com.prajwalch.torrentsearch/.MainActivity
```

**Configuration:**
- `compileSdk = 36`, `minSdk = 25` (Android 7.1+), Kotlin JVM target 11
- Compose BOM: `2025.12.00`, Kotlin: `2.2.21`, Room: `2.8.4`, Hilt: `2.57.2`, Ktor: `3.3.3`
- Version in `app/build.gradle.kts`: `versionName = "0.4.4"`
- KSP (Kotlin Symbol Processing) for Room/Hilt code generation

### Testing

- **Unit tests** in `app/src/test/` (JUnit 4)
- **Instrumentation tests** in `app/src/androidTest/` (Espresso for UI)
- Run: `./gradlew test` (unit) or `./gradlew connectedAndroidTest` (instrumentation)
- Provider parsing: Mock HTTP responses; never rely on live scraping in tests

### Adding a New Torrent Provider

1. **Create file** in `providers/YourProvider.kt`
2. **Implement** `SearchProvider` interface:
   ```kotlin
   class YourProvider : SearchProvider {
       override val info = SearchProviderInfo(
           id = "your_provider",
           name = "Your Provider",
           url = "https://example.com",
           specializedCategory = Category.All,
           safetyStatus = SearchProviderSafetyStatus.Safe,
           enabledByDefault = false,  // Disable by default for new providers
           type = SearchProviderType.Builtin,
       )
       
       override suspend fun search(query: String, context: SearchContext): List<Torrent> {
           // Use context.httpClient (Ktor) for GET/POST
           val response = context.httpClient.get(url = "...")
           // Parse with jsoup (HTML) or kotlinx.serialization (JSON)
           // Return List<Torrent> or throw—framework wraps as SearchProviderException
           return parseResults(response)
       }
   }
   ```
3. **Register** in `SearchProvidersRepository.builtins` list
4. **Test** in isolation: mock `context.httpClient.get()` response, verify parsing
5. **Example providers:** `ThePirateBay.kt` (HTML scrape), `Nyaa.kt` (JSON API), `Torznab.kt` (generic Torznab API)

**Provider Patterns:**
- Use `context.httpClient` (Ktor HttpClient singleton, 3 retries, 20s timeout)
- Parse HTML with `jsoup`, JSON with `kotlinx.serialization`
- CPU-intensive parsing: wrap in `withContext(Dispatchers.Default)` (see `LimeTorrents.kt`)
- Parallel searches: use `coroutineScope { async { ... }.awaitAll() }` (see `BitSearch.kt`)
- Return empty list if no results; exceptions automatically wrapped in `SearchProviderException` by `TorrentsRemoteDataSource`

### External Providers: Torznab (Jackett/Prowlarr)

**TorznabSearchProvider** in `providers/Torznab.kt`:
- Dynamically created from `TorznabConfigEntity` stored in Room DB
- Parsed by `SearchProvidersRepository` via `torznabConfigDao.observeAll()` → combined with builtins
- Each config includes: `id`, `name`, `baseUrl`, `apiKey`, `category mappings`
- Constructs Torznab API request with query encoding, category, and pagination
- Parses XML response (Torznab spec) into `Torrent` objects
- **Editing config:** Use `SearchProvidersViewModel` in settings UI; persists to Room DB
- Failures wrapped same as builtin providers; no special error handling needed

---

## Key Conventions & Patterns

### 1. **Type Aliases for Domain Clarity**

```kotlin
typealias MagnetUri = String
typealias SearchProviderId = String
typealias MaxNumResults = Int  // wrapped in models/
```

Use instead of raw Strings to prevent mixing up different string domains.

### 2. **Sealed Classes for Exhaustive Handling**

```kotlin
// In Torrent.kt
sealed class InfoHashOrMagnetUri {
    data class InfoHash(val hash: String) : InfoHashOrMagnetUri()
    data class MagnetUri(val uri: String) : InfoHashOrMagnetUri()
}

// Compiler forces handling both cases
fun magnetUri(): MagnetUri = when (infoHashOrMagnetUri) {
    is InfoHash -> createMagnetUri(infoHashOrMagnetUri.hash)
    is MagnetUri -> infoHashOrMagnetUri.uri
}
```

### 3. **Immutable Collections**

Use `ImmutableList` (from `kotlinx.collections.immutable`) in public APIs for thread safety:
```kotlin
data class SearchResults(
    val successes: ImmutableList<Torrent>,
    val failures: ImmutableList<Throwable>,
)
```

Convert with `.toImmutableList()` before emitting from flows.

### 4. **Hilt Dependency Injection Modules**

Located in `di/`:
- `RoomModule.kt` - Database singleton
- `HttpClientModule.kt` - HTTP client singleton
- `DataStoreModule.kt` - User preferences (encrypted)
- `ConnectivityModule.kt` - Network state checking

Inject directly: `class MyClass @Inject constructor(repository: MyRepository)`

### 5. **Safe Mode Implementation**

Settings gate unsafe providers and NSFW content:
```kotlin
// In SearchTorrentsUseCase
val enabledSearchProviders = getEnabledSearchProviders(category)
// Filters based on settingsRepository.safeMode and provider.safetyStatus
```

UI reflects this: NSFW torrents hidden/flagged when safe mode active.

### 6. **Error Handling Pattern**

Exceptions wrapped in domain model:
```kotlin
class SearchProviderException(
    val id: SearchProviderId,
    val name: String,
    val url: String,
    override val cause: Throwable,
) : Exception(...)
```

UI collects failures in `SearchResults.failures` and displays to user without crashing.

---

## Critical Integration Points

### Network Layer (Ktor HttpClient)

Located in `network/HttpClient.kt`:
- Singleton with retry logic (3 retries for timeouts)
- Timeouts: 10s connection, 15s socket, 20s total request
- User-Agent, headers configured per request
- Passed to providers via `SearchContext`

**Do NOT create separate HTTP clients; reuse `HttpClient` object.**

### Settings Persistence (DataStore)

Located in `data/repository/SettingsRepository.kt`:
- Stores: theme mode, safe mode, provider enable/disable state, search history preference
- Encrypted on API 30+
- Flows emitted for reactive updates: `enableNSFWMode`, `darkTheme`, etc.

Add new settings by extending `SettingsRepository` and emitting from `DataStore<Preferences>`.

### Database Migrations

When modifying `BookmarkedTorrent`, `SearchHistoryEntity`, or `TorznabConfigEntity`:
1. Bump `version` in `@Database` annotation
2. Create migration spec if auto-migration insufficient (see `Migration2To3Spec`)
3. Export schema: `exportSchema = true` stores JSON snapshots in `app/schemas/`
4. Test migrations on old device installations

---

## Gradle & Dependency Management

**Central version management** via `gradle/libs.versions.toml` (single source of truth):
- Compose BOM: `2025.12.00` (Material 3)
- Kotlin: `2.2.21`
- Room: `2.8.4` (with KSP compiler)
- Hilt: `2.57.2`
- Ktor: `3.3.3` (HttpClient)
- kotlinx-collections-immutable: `0.4.0`
- jsoup: `1.21.2` (HTML parsing)

Plugins: KSP (code generation), Room (entity scaffolding), Hilt (DI), Android Gradle Plugin 8.11.1

**Never add dependencies directly in build.gradle.kts; update TOML first, then reference.**

---

## Translation & Localization

- English strings in `res/values/strings.xml`
- Translations managed via **Weblate** for: Russian, Ukrainian, Spanish, German, French, Polish, Portuguese (BR), Hindi, Arabic, Farsi, Chinese (Simplified), Japanese
- Add new strings to `strings.xml` with clear keys; Weblate auto-picks them up

---

## Common Pitfalls & Best Practices

1. **Never block UI threads** - All repository/network calls must use `Dispatchers.IO` (via `flowOn(Dispatchers.IO)`)
2. **Preserve incremental streaming** - Never wait for all providers; results emit as each completes via `channelFlow`
3. **NSFW filtering must be consistent** - Apply both in use case (`getEnabledSearchProviders`) AND UI layer (NSFW torrents hidden)
4. **Test provider parsing in isolation** - Mock HTTP via test double `HttpClient`, never scrape live sites in tests
5. **Use type aliases** - Prevent mixing domains: `SearchProviderId` ≠ query `String` ≠ `MagnetUri`
6. **Reuse HttpClient singleton** - Inject from `@Module HttpClientModule`, never create new instances
7. **Immutable collections everywhere** - All public APIs use `ImmutableList`; convert with `.toImmutableList()` before emitting
8. **Don't mutate SearchResults** - Create new instances with `.copy(successes = ...)` to preserve immutability
9. **Cancel flows on ViewModel clear** - `stateIn()` with `viewModelScope` auto-cancels; don't manually subscribe outside ViewModel
10. **Settings flows are reactive** - Changes to `SettingsRepository.*` automatically propagate to UI via `combine()`; no manual refresh needed

---

## File Structure Quick Reference

```
app/src/main/kotlin/com/prajwalch/torrentsearch/
├── TorrentSearch.kt                    # @HiltAndroidApp entry point
├── providers/                          # 18 SearchProvider implementations
│   ├── SearchProvider.kt               # Interface, SearchProviderInfo, SafetyStatus
│   ├── SearchContext.kt                # Context passed to search() (httpClient, category)
│   ├── SearchProviderType.kt           # Builtin vs External (Torznab)
│   ├── ThePirateBay.kt, Nyaa.kt, ...  # Builtin scrapers (HTML/JSON)
│   └── Torznab.kt                      # Generic Jackett/Prowlarr adapter
├── data/
│   ├── repository/                     # 6 repos (TorrentsRepository, SettingsRepository, etc.)
│   │   └── SearchProvidersRepository   # Builds provider list (builtins + Torznab configs from DB)
│   ├── remote/TorrentsRemoteDataSource # channelFlow parallel search across providers
│   └── local/
│       ├── entities/                   # BookmarkedTorrent, SearchHistoryEntity, TorznabConfigEntity
│       └── dao/                        # *Dao interfaces for Room queries
├── network/HttpClient.kt               # Ktor client singleton (retry, timeout config)
├── ui/
│   ├── search/SearchViewModel.kt       # Orchestrates search, applies sorting/filtering
│   ├── search/SearchScreen.kt          # Compose UI, search bar, result list
│   ├── home/HomeViewModel.kt           # Search history & category selection
│   ├── settings/                       # Theme, safe mode, provider toggles
│   └── MainViewModel.kt                # App-level state (theme, connectivity)
├── usecases/SearchTorrentsUseCase.kt   # Applies provider filtering, result limits, cancellation
├── models/                             # Torrent, Category, SearchResults, etc. (data classes)
├── di/                                 # RoomModule, HttpClientModule, DataStoreModule, etc.
├── extensions/                         # Flow extensions, serialization helpers
└── utils/                              # Sorting, file size parsing, string resources
```

**Key Data Classes:**
- `Torrent` - title, magnetUri, seeders, peers, uploadDate, category, isNSFW, provider
- `SearchProviderInfo` - id, name, url, specializedCategory, safetyStatus, enabledByDefault, type
- `SearchResults` - successes: ImmutableList<Torrent>, failures: ImmutableList<Throwable>

---

## Testing Strategy

1. **Unit tests** in `app/src/test/` - Test repository logic, use case filtering, ViewModel state derivation
2. **Provider parsing tests** - Mock HTTP responses; verify parsing without live network
3. **UI tests** in `app/src/androidTest/` - Espresso for navigation, user interactions
4. **Edge cases:** empty results, network timeouts, malformed HTML, NSFW filtering, provider failures

Run all: `./gradlew test connectedAndroidTest`
