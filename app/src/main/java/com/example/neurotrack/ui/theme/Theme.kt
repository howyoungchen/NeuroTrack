package com.example.neurotrack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    secondary = BlueGrey80,
    tertiary = Sky80,
    background = Color(0xFF101417),
    surface = Color(0xFF101417),
    surfaceContainerLow = Color(0xFF171C1F),
    surfaceContainer = Color(0xFF1D2326),
    surfaceVariant = Color(0xFF3F484B),
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F3EC),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = BlueGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE8EC),
    onSecondaryContainer = Color(0xFF071F25),
    tertiary = Sky40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E3FF),
    onTertiaryContainer = Color(0xFF001B3F),
    background = Color(0xFFFAFCFF),
    onBackground = Color(0xFF191C1F),
    surface = Color(0xFFFAFCFF),
    onSurface = Color(0xFF191C1F),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF2F6F8),
    surfaceVariant = Color(0xFFDCE4E7),
    onSurfaceVariant = Color(0xFF40484B),
    outline = Color(0xFF6F797C),
    outlineVariant = Color(0xFFC0C8CB),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

@Composable
fun NeuroTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
