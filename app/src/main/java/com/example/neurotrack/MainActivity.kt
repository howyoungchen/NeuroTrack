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
import androidx.lifecycle.ViewModelProvider
import com.example.neurotrack.background.EXTRA_DESTINATION
import com.example.neurotrack.ui.NeuroTrackRoot
import com.example.neurotrack.ui.NeuroTrackViewModel
import com.example.neurotrack.ui.destinationToScreen
import com.example.neurotrack.ui.theme.NeuroTrackTheme

class MainActivity : ComponentActivity() {
    private lateinit var neuroTrackViewModel: NeuroTrackViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        neuroTrackViewModel = ViewModelProvider(
            this,
            NeuroTrackViewModel.factory(application),
        )[NeuroTrackViewModel::class.java]
        val initialScreen = destinationToScreen(intent?.getStringExtra(EXTRA_DESTINATION))
        setContent {
            val settings by neuroTrackViewModel.settings.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                SettingsStore.THEME_LIGHT -> false
                SettingsStore.THEME_DARK -> true
                else -> systemDark
            }
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                )
            }
            NeuroTrackTheme(darkTheme = darkTheme) {
                NeuroTrackRoot(neuroTrackViewModel, initialScreen)
            }
        }
    }

    override fun onStop() {
        if (!isChangingConfigurations) neuroTrackViewModel.interruptMindfulness()
        super.onStop()
    }
}
