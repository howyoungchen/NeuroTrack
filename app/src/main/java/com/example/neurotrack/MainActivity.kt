package com.example.neurotrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.neurotrack.background.EXTRA_DESTINATION
import com.example.neurotrack.ui.NeuroTrackRoot
import com.example.neurotrack.ui.NeuroTrackViewModel
import com.example.neurotrack.ui.destinationToScreen
import com.example.neurotrack.ui.theme.NeuroTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialScreen = destinationToScreen(intent?.getStringExtra(EXTRA_DESTINATION))
        setContent {
            val viewModel: NeuroTrackViewModel = viewModel(
                factory = NeuroTrackViewModel.factory(application),
            )
            val settings by viewModel.settings.collectAsState()
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                SettingsStore.THEME_LIGHT -> false
                SettingsStore.THEME_DARK -> true
                else -> systemDarkTheme
            }
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = android.graphics.Color.TRANSPARENT,
                        darkScrim = android.graphics.Color.TRANSPARENT,
                        detectDarkMode = { darkTheme },
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = android.graphics.Color.TRANSPARENT,
                        darkScrim = android.graphics.Color.TRANSPARENT,
                        detectDarkMode = { darkTheme },
                    ),
                )
            }
            NeuroTrackTheme(darkTheme = darkTheme) {
                NeuroTrackRoot(
                    viewModel = viewModel,
                    initialScreen = initialScreen,
                )
            }
        }
    }
}
