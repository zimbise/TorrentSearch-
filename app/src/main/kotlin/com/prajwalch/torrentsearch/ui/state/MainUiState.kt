package com.prajwalch.torrentsearch.ui.state

import com.prajwalch.torrentsearch.models.DarkTheme

data class MainUiState(
    val darkTheme: DarkTheme = DarkTheme.FollowSystem,
    val enableDynamicTheme: Boolean = true,
    val pureBlack: Boolean = false,
)
