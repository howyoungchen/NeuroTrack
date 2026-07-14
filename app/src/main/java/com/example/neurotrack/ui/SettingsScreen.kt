package com.example.neurotrack.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.neurotrack.AppSettings
import com.example.neurotrack.BuildConfig
import com.example.neurotrack.R
import com.example.neurotrack.SettingsStore
import com.example.neurotrack.domain.MindfulnessSchedule
import java.time.DayOfWeek

private enum class SettingsSheet {
    REFRESH_DAY,
    LANGUAGE,
    THEME,
    ABOUT,
}

private data class SettingOption<T>(
    val value: T,
    val label: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    settings: AppSettings,
    onRefreshDay: (DayOfWeek) -> Unit,
    onLanguage: (String) -> Unit,
    onTheme: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val refreshDayLabel = weekdayLabel(settings.refreshDay)
    val assessmentDayLabel = weekdayLabel(MindfulnessSchedule.assessmentDay(settings.refreshDay))
    val languageLabel = stringResource(
        if (settings.languageTag == SettingsStore.LANGUAGE_EN) R.string.language_en else R.string.language_zh,
    )
    val themeLabel = stringResource(
        when (settings.themeMode) {
            SettingsStore.THEME_LIGHT -> R.string.theme_light
            SettingsStore.THEME_DARK -> R.string.theme_dark
            else -> R.string.theme_system
        },
    )
    var notificationGranted by rememberSaveable { mutableStateOf(context.notificationsGranted()) }
    var openSheetName by rememberSaveable { mutableStateOf<String?>(null) }
    val openSheet = openSheetName?.let { name -> SettingsSheet.entries.firstOrNull { it.name == name } }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationGranted = it
    }

    PageList {
        item {
            PageHeader(
                R.string.settings_title,
                stringResource(R.string.settings_subtitle),
                Icons.Rounded.Settings,
            )
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_rhythm)) {
                SettingsRow(
                    icon = Icons.Rounded.Timeline,
                    title = stringResource(R.string.settings_refresh_day),
                    summary = stringResource(
                        R.string.settings_refresh_day_summary,
                        refreshDayLabel,
                        assessmentDayLabel,
                    ),
                    value = refreshDayLabel,
                    onClick = { openSheetName = SettingsSheet.REFRESH_DAY.name },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.settings_reminder),
                    summary = stringResource(R.string.settings_reminder_days, assessmentDayLabel),
                    value = stringResource(
                        if (notificationGranted) R.string.permission_granted else R.string.permission_missing,
                    ),
                    enabled = !notificationGranted,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_preferences)) {
                SettingsRow(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.settings_language),
                    summary = stringResource(R.string.settings_language_desc),
                    value = languageLabel,
                    onClick = { openSheetName = SettingsSheet.LANGUAGE.name },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_theme),
                    summary = stringResource(R.string.settings_theme_desc),
                    value = themeLabel,
                    onClick = { openSheetName = SettingsSheet.THEME.name },
                )
            }
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_information)) {
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_about),
                    summary = stringResource(R.string.settings_about_desc),
                    value = BuildConfig.VERSION_NAME,
                    onClick = { openSheetName = SettingsSheet.ABOUT.name },
                )
            }
        }
    }

    when (openSheet) {
        SettingsSheet.REFRESH_DAY -> SettingsOptionSheet(
            title = stringResource(R.string.settings_choose_refresh_day),
            description = stringResource(
                R.string.settings_refresh_day_desc,
                refreshDayLabel,
                assessmentDayLabel,
            ),
            selectedValue = settings.refreshDay,
            options = DayOfWeek.entries.map { day -> SettingOption(day, weekdayLabel(day)) },
            onSelect = { day ->
                openSheetName = null
                onRefreshDay(day)
            },
            onDismiss = { openSheetName = null },
        )

        SettingsSheet.LANGUAGE -> SettingsOptionSheet(
            title = stringResource(R.string.settings_choose_language),
            description = stringResource(R.string.settings_language_desc),
            selectedValue = settings.languageTag,
            options = listOf(
                SettingOption(SettingsStore.LANGUAGE_ZH, stringResource(R.string.language_zh)),
                SettingOption(SettingsStore.LANGUAGE_EN, stringResource(R.string.language_en)),
            ),
            onSelect = { language ->
                openSheetName = null
                onLanguage(language)
            },
            onDismiss = { openSheetName = null },
        )

        SettingsSheet.THEME -> SettingsOptionSheet(
            title = stringResource(R.string.settings_choose_theme),
            description = stringResource(R.string.settings_theme_desc),
            selectedValue = settings.themeMode,
            options = listOf(
                SettingOption(SettingsStore.THEME_SYSTEM, stringResource(R.string.theme_system)),
                SettingOption(SettingsStore.THEME_LIGHT, stringResource(R.string.theme_light)),
                SettingOption(SettingsStore.THEME_DARK, stringResource(R.string.theme_dark)),
            ),
            onSelect = { theme ->
                openSheetName = null
                onTheme(theme)
            },
            onDismiss = { openSheetName = null },
        )

        SettingsSheet.ABOUT -> AboutSheet(onDismiss = { openSheetName = null })
        null -> Unit
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    summary: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .heightIn(min = 72.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (enabled) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        modifier = Modifier.padding(start = 70.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsOptionSheet(
    title: String,
    description: String,
    selectedValue: T,
    options: List<SettingOption<T>>,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                options.forEach { option ->
                    val selected = option.value == selectedValue
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.value) },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null)
                    }
                }
                Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    stringResource(R.string.settings_local_only),
                    style = MaterialTheme.typography.bodyLarge,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    stringResource(R.string.disclaimer),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

private fun Context.notificationsGranted(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
