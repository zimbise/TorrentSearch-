package com.prajwalch.torrentsearch.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.DarkTheme
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var startDestination = Screens.HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate() called")

        installSplashScreen()
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) { moveTaskToBack(true) }
        handleIntent()

        enableEdgeToEdge()
        setContent {
            val mainViewModel = hiltViewModel<MainViewModel>()

            // ✅ AUTO‑SYNC JACKETT ON APP START
            LaunchedEffect(Unit) {
                mainViewModel.syncAllJackettConfigs()
            }

            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

            val darkTheme = when (uiState.darkTheme) {
                DarkTheme.On -> true
                DarkTheme.Off -> false
                DarkTheme.FollowSystem -> isSystemInDarkTheme()
            }

            TorrentSearchTheme(
                darkTheme = darkTheme,
                dynamicColor = uiState.enableDynamicTheme,
                pureBlack = uiState.pureBlack,
            ) {
                TorrentSearchApp(
                    onDownloadTorrent = ::downloadTorrentViaClient,
                    onShareMagnetLink = ::shareMagnetLink,
                    onOpenDescriptionPage = ::openDescriptionPage,
                    onShareDescriptionPageUrl = ::shareDescriptionPageUrl,
                    startDestination = startDestination,
                )
            }
        }
    }

    private fun handleIntent() {
        val intent = intent ?: return
        val action = intent.action
        val type = intent.type

        when (action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == type) {
                    handleSendText(intent)
                }
            }

            Intent.ACTION_PROCESS_TEXT -> {
                if ("text/plain" == type) {
                    handleProcessText(intent)
                }
            }

            else -> {}
        }
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            Log.i(TAG, "Received '$it' from Intent.ACTION_SEND")
            changeStartDestinationToSearch(searchQuery = it)
        }
    }

    private fun handleProcessText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)?.let {
            Log.i(TAG, "Received '$it' from Intent.ACTION_PROCESS_TEXT")
            changeStartDestinationToSearch(searchQuery = it)
        }
    }

    private fun changeStartDestinationToSearch(searchQuery: String) {
        if (searchQuery.isBlank()) {
            val msg = getString(R.string.main_cannot_search_blank_query_message)
            showToast(msg)
            return
        }

        val urlPatternMatcher = Patterns.WEB_URL.matcher(searchQuery)

        if (urlPatternMatcher.matches()) {
            val msg = getString(R.string.main_cannot_search_using_url_message)
            showToast(msg)
            return
        }

        val cleaned = urlPatternMatcher.replaceAll("").trim().trim('"', '\n')
        Log.d(TAG, "Performing search; query = $cleaned")

        startDestination = Screens.createSearchRoute(
            query = cleaned,
            category = Category.All,
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun downloadTorrentViaClient(magnetUri: MagnetUri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, magnetUri.toUri())
        return try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
