package com.example.neurotrack.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = INDIGO_80,
    onPrimary = INDIGO_20,
    primaryContainer = INDIGO_30,
    onPrimaryContainer = INDIGO_90,
    secondary = MINT_80,
    onSecondary = Color(0xFF00382D),
    secondaryContainer = Color(0xFF124F41),
    onSecondaryContainer = MINT_90,
    tertiary = AMBER_80,
    onTertiary = Color(0xFF4B3700),
    tertiaryContainer = Color(0xFF654C00),
    onTertiaryContainer = AMBER_90,
    background = NIGHT,
    onBackground = Color(0xFFE5E1EC),
    surface = NIGHT,
    onSurface = Color(0xFFE5E1EC),
    surfaceContainerLowest = Color(0xFF0B0C12),
    surfaceContainerLow = NIGHT_RAISED,
    surfaceContainer = NIGHT_MUTED,
    surfaceContainerHigh = Color(0xFF2A2B37),
    surfaceVariant = Color(0xFF454552),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF91909B),
    outlineVariant = Color(0xFF454550),
    error = DANGER_DARK,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColorScheme = lightColorScheme(
    primary = INDIGO_40,
    onPrimary = Color.White,
    primaryContainer = INDIGO_90,
    onPrimaryContainer = INDIGO_10,
    secondary = MINT_40,
    onSecondary = Color.White,
    secondaryContainer = MINT_90,
    onSecondaryContainer = Color(0xFF002018),
    tertiary = AMBER_40,
    onTertiary = Color.White,
    tertiaryContainer = AMBER_90,
    onTertiaryContainer = Color(0xFF241A00),
    background = PAPER,
    onBackground = INK,
    surface = PAPER,
    onSurface = INK,
    surfaceContainerLowest = PAPER_RAISED,
    surfaceContainerLow = PAPER_RAISED,
    surfaceContainer = PAPER_MUTED,
    surfaceContainerHigh = Color(0xFFE9E9F2),
    surfaceVariant = Color(0xFFE4E3EC),
    onSurfaceVariant = INK_MUTED,
    outline = Color(0xFF777783),
    outlineVariant = Color(0xFFC9C7D1),
    error = DANGER_LIGHT,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val appShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun NeuroTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
        shapes = appShapes,
        content = content,
    )
}
