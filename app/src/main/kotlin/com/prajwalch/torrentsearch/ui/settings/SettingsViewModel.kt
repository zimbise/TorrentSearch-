package com.prajwalch.torrentsearch.ui.settings

import android.content.ComponentName
import android.content.pm.PackageManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.BuildConfig
import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.DarkTheme
import com.prajwalch.torrentsearch.models.MaxNumResults
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOptions
import com.prajwalch.torrentsearch.models.SortOrder

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/** State for the appearance settings. */
data class AppearanceSettingsUiState(
    val enableDynamicTheme: Boolean = true,
    val darkTheme: DarkTheme = DarkTheme.FollowSystem,
    val pureBlack: Boolean = false,
)

/** State for the general settings. */
data class GeneralSettingsUiState(
    val enableNSFWMode: Boolean = false,
)

/** State for the search settings. */
data class SearchSettingsUiState(
    val searchProvidersStat: SearchProvidersStat = SearchProvidersStat(),
    val defaultCategory: Category = Category.All,
    val defaultSortOptions: SortOptions = SortOptions(),
    val maxNumResults: MaxNumResults = MaxNumResults.Unlimited,
)

data class SearchProvidersStat(
    val enabledSearchProvidersCount: Int = 0,
    val totalSearchProvidersCount: Int = 0,
)

/** State for the search history settings. */
data class SearchHistorySettingsUiState(
    val saveSearchHistory: Boolean = true,
    val showSearchHistory: Boolean = true,
)

data class AdvanceSettingsUiState(
    val enableShareIntegration: Boolean = true,
    val enableQuickSearch: Boolean = true,
)

/** ViewModel that handles the business logic of Settings screen. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val searchProvidersRepository: SearchProvidersRepository,
) : ViewModel() {
    val appearanceSettingsUiState = combine(
        settingsRepository.enableDynamicTheme,
        settingsRepository.darkTheme,
        settingsRepository.pureBlack,
        ::AppearanceSettingsUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = AppearanceSettingsUiState(),
    )

    val generalSettingsUiState = settingsRepository
        .enableNSFWMode
        .map(::GeneralSettingsUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = GeneralSettingsUiState(),
        )

    private val searchProvidersStatFlow = combine(
        settingsRepository.enabledSearchProvidersId.map { it.size },
        searchProvidersRepository.observeSearchProvidersCount(),
        ::SearchProvidersStat,
    )
    val searchSettingsUiState = combine(
        searchProvidersStatFlow,
        settingsRepository.defaultCategory,
        settingsRepository.defaultSortOptions,
        settingsRepository.maxNumResults,
        ::SearchSettingsUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = SearchSettingsUiState(),
    )

    val searchHistorySettingsUiState = combine(
        settingsRepository.saveSearchHistory,
        settingsRepository.showSearchHistory,
        ::SearchHistorySettingsUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = SearchHistorySettingsUiState(),
    )

    val advanceSettingsUiState = combine(
        settingsRepository.enableShareIntegration,
        settingsRepository.enableQuickSearch,
        ::AdvanceSettingsUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = AdvanceSettingsUiState(),
    )

    /** Enables/disables dynamic theme. */
    fun enableDynamicTheme(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableDynamicTheme(enable = enable)
        }
    }

    /** Changes the dark theme mode. */
    fun setDarkTheme(darkTheme: DarkTheme) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(darkTheme = darkTheme)
        }
    }

    /** Enables/disables pure black mode. */
    fun enablePureBlackTheme(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.enablePureBlack(enable = enable)
        }
    }

    /** DISABLED: No content restrictions - all content shown by default. */
    fun enableNSFWMode(enable: Boolean) {
        // No-op: Content restrictions removed. Custom restrictions applied at config level only.
        // Method kept for API compatibility.
    }

    /** Changes the default category to given one. */
    fun setDefaultCategory(category: Category) {
        viewModelScope.launch {
            settingsRepository.setDefaultCategory(category = category)
        }
    }

    // REMOVED: disableRestrictedSearchProviders() - No content restrictions
    // All providers enabled by default regardless of safety status
    // Custom restrictions applied at configuration level only

    /** Changes the default sort criteria. */
    fun setDefaultSortCriteria(sortCriteria: SortCriteria) {
        viewModelScope.launch {
            settingsRepository.setDefaultSortCriteria(sortCriteria = sortCriteria)
        }
    }

    /** Changes the default sort order. */
    fun setDefaultSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            settingsRepository.setDefaultSortOrder(sortOrder = sortOrder)
        }
    }

    /** Updates the maximum number of results. */
    fun setMaxNumResults(maxNumResults: MaxNumResults) {
        viewModelScope.launch {
            settingsRepository.setMaxNumResults(maxNumResults = maxNumResults)
        }
    }

    /** Saves/unsaves search history. */
    fun enableSaveSearchHistory(save: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableSaveSearchHistory(enable = save)
        }
    }

    /** Shows/hides search history. */
    fun enableShowSearchHistory(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableShowSearchHistory(show = show)
        }
    }

    fun enableShareIntegration(enable: Boolean, packageManager: PackageManager) {
        viewModelScope.launch {
            enableIntentIntegration(
                enable = enable,
                packageManager = packageManager,
                activityAliasName = ".SendAlias",
            )
            settingsRepository.enableShareIntegration(enable = enable)
        }
    }

    fun enableQuickSearch(enable: Boolean, packageManager: PackageManager) {
        viewModelScope.launch {
            enableIntentIntegration(
                enable = enable,
                packageManager = packageManager,
                activityAliasName = ".ProcessTextAlias",
            )
            settingsRepository.enableQuickSearch(enable = enable)
        }
    }

    private fun enableIntentIntegration(
        enable: Boolean,
        packageManager: PackageManager,
        activityAliasName: String,
    ) {
        val packageName = BuildConfig.APPLICATION_ID
        val componentName = ComponentName(packageName, "$packageName$activityAliasName")

        val componentEnabledState = if (enable) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        packageManager.setComponentEnabledSetting(
            componentName,
            componentEnabledState,
            PackageManager.DONT_KILL_APP,
        )
    }
}