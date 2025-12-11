package com.prajwalch.torrentsearch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.ui.state.MainUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: SearchProvidersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    // ✅ AUTO‑SYNC FUNCTION CALLED FROM MainActivity
    fun syncAllJackettConfigs() {
        viewModelScope.launch {
            try {
                repository.syncAllJackettConfigs()
            } catch (_: Exception) {
                // ignore silently
            }
        }
    }
}
