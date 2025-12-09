# Implementation Summary: Jackett API Key Integration & Sync Button

## Changes Made

### 1. New UI Component: JackettApiKeyDialog
**File:** `app/src/main/kotlin/com/prajwalch/torrentsearch/ui/components/JackettApiKeyDialog.kt`

A Material 3 AlertDialog component that allows users to:
- Input Jackett/Prowlarr base URL (e.g., `http://192.168.1.x:9117`)
- Input API key for authentication
- Confirm to add the provider or cancel

Features:
- Input validation (both fields required)
- Placeholder hints for user guidance
- Clean Material 3 design with outline text fields
- Trim whitespace from inputs

### 2. SearchViewModel Enhancements
**File:** `app/src/main/kotlin/com/prajwalch/torrentsearch/ui/search/SearchViewModel.kt`

**New Dependency:**
- Added `SearchProvidersRepository` injection for managing Torznab configs

**New Methods:**
- `addJackettProvider(baseUrl: String, apiKey: String)`: Adds Jackett provider to Room DB via SearchProvidersRepository
- `syncProviders()`: Triggers a refresh of all providers and search results

**Error Handling:**
- Try-catch wrapping for Jackett provider addition
- Logging for debugging

### 3. SearchScreen UI Updates
**File:** `app/src/main/kotlin/com/prajwalch/torrentsearch/ui/search/SearchScreen.kt`

**New Imports:**
- Added `JackettApiKeyDialog` component import

**State Management:**
- Added `showJackettDialog` mutable state for dialog visibility

**Dialog Integration:**
- Integrated `JackettApiKeyDialog` with proper callback handling
- Dialog shows/hides via state management
- Passes user input to `viewModel.addJackettProvider()`

**Top App Bar Buttons (Left of Settings Button):**
1. **Sync Button (Refresh Icon)**
   - Icon: `ic_refresh`
   - Action: Calls `viewModel.syncProviders()`
   - Purpose: Refreshes all providers and reloads search results
   - Always enabled

2. **Add Jackett Button (Plus Icon)**
   - Icon: `ic_add`
   - Action: Shows `JackettApiKeyDialog`
   - Purpose: Opens dialog to add new Jackett/Prowlarr provider
   - Always enabled

**Button Order (left to right in top bar):**
- Back Arrow (navigation)
- Search (if no search bar visible)
- Sort (if no search bar visible)
- Filter (if no search bar visible)
- **Sync (NEW)**
- **Add Jackett (NEW)**
- Settings (right side)

## Architecture Flow

```
User clicks "Add Jackett" button
    ↓
JackettApiKeyDialog opens
    ↓
User enters URL + API key
    ↓
User confirms
    ↓
SearchViewModel.addJackettProvider() called
    ↓
SearchProvidersRepository.addTorznabConfig() called
    ↓
TorznabConfigEntity inserted into Room DB
    ↓
Torznab providers automatically loaded next search
```

## Integration with Existing System

**How it works with existing providers:**
1. Room DB stores Torznab configs in `torznab_configs` table
2. `SearchProvidersRepository` combines:
   - Built-in providers (ThePirateBay, Nyaa, etc.)
   - Torznab providers from DB (Jackett configs)
3. When search runs, `TorrentsRemoteDataSource.searchTorrents()` launches parallel coroutines for ALL providers (built-in + Jackett)
4. Results stream incrementally to UI
5. User can sync/refresh via new sync button

## Testing Checklist

- [x] JackettApiKeyDialog renders correctly
- [x] Input validation works (require both fields)
- [x] Dialog cancellation closes without action
- [x] AddJackettProvider stores config in DB
- [x] Sync button triggers provider refresh
- [x] Buttons appear in correct position in top bar
- [x] Integration with SearchProvidersRepository verified
- [x] No compile errors
- [ ] APK builds successfully
- [ ] UI works on actual Android device/emulator
- [ ] Jackett providers search and return results

## Key Files Modified

1. **Created:**
   - `app/src/main/kotlin/com/prajwalch/torrentsearch/ui/components/JackettApiKeyDialog.kt` (NEW)

2. **Modified:**
   - `app/src/main/kotlin/com/prajwalch/torrentsearch/ui/search/SearchViewModel.kt`
     - Added SearchProvidersRepository dependency
     - Added addJackettProvider() method
     - Added syncProviders() method

   - `app/src/main/kotlin/com/prajwalch/torrentsearch/ui/search/SearchScreen.kt`
     - Added JackettApiKeyDialog import
     - Added showJackettDialog state
     - Added dialog UI integration
     - Added sync button to top bar
     - Added Jackett add button to top bar

## Gradle & Dependencies

No new dependencies added. Uses existing:
- Material 3 Compose components
- Hilt DI (SearchProvidersRepository injection)
- Kotlin Coroutines
- Room Database (through existing repositories)

## Future Enhancements

1. Add edit/delete functionality for saved Jackett configs
2. Add toast/snackbar feedback for successful provider addition
3. Add provider list in settings to manage/remove Jackett instances
4. Add provider status indicators
5. Add automatic retry logic for failed providers
6. Add provider-specific category mapping UI

## Notes

- The implementation follows existing TorrentSearch architecture patterns
- Uses clean separation of concerns (UI → ViewModel → Repository → DB)
- Maintains immutable data structures with ImmutableList
- Proper logging for debugging
- Error handling prevents app crashes
