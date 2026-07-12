package com.example.neurotrack.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.neurotrack.BuildConfig
import com.example.neurotrack.R
import com.example.neurotrack.SettingsStore
import com.example.neurotrack.background.LocationSleepSignalReader
import com.example.neurotrack.background.PermissionIntents
import com.example.neurotrack.domain.StressBand
import com.example.neurotrack.domain.StressResult
import com.example.neurotrack.domain.StressTrendPoint
import java.util.Locale
import kotlin.math.roundToInt

enum class AppScreen(val titleRes: Int) {
    Status(R.string.nav_status),
    Assessment(R.string.nav_assessment),
    Settings(R.string.nav_settings),
}

fun destinationToScreen(destination: String?): AppScreen =
    if (destination == "assessment") AppScreen.Assessment else AppScreen.Status

@Composable
fun NeuroTrackRoot(
    viewModel: NeuroTrackViewModel,
    initialScreen: AppScreen = AppScreen.Status,
) {
    val settings by viewModel.settings.collectAsState()
    var selectedScreen by rememberSaveable { mutableStateOf(initialScreen) }

    LocalizedResources(settings.languageTag) {
        val uiState by viewModel.uiState.collectAsState()
        val latestSubmission by viewModel.latestSubmission.collectAsState()
        val useNavigationRail = LocalConfiguration.current.screenWidthDp >= 600

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useNavigationRail) {
                    AppBottomNavigation(
                        selectedScreen = selectedScreen,
                        onSelect = { selectedScreen = it },
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                if (useNavigationRail) {
                    AppNavigationRail(
                        selectedScreen = selectedScreen,
                        onSelect = { selectedScreen = it },
                    )
                }
                AnimatedContent(
                    targetState = selectedScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
                    },
                    label = "screen",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) { screen ->
                    when (screen) {
                        AppScreen.Assessment -> AssessmentScreen(
                            uiState = uiState,
                            latestSubmission = latestSubmission,
                            onSubmit = viewModel::submitAssessment,
                            onDismissResult = viewModel::clearLatestSubmission,
                        )

                        AppScreen.Status -> StatusScreen(
                            status = uiState.status,
                            onStartAssessment = { selectedScreen = AppScreen.Assessment },
                        )

                        AppScreen.Settings -> SettingsScreen(
                            settings = settings,
                            onLanguageChange = viewModel::setLanguage,
                            onThemeModeChange = viewModel::setThemeMode,
                            onReminderChange = viewModel::setReminder,
                            onExportLogs = viewModel::exportLogs,
                            onExportSleepRawData = viewModel::exportSleepRawData,
                        )
                    }
                }
            }
        }
    }
}

private val appScreens = listOf(AppScreen.Status, AppScreen.Assessment, AppScreen.Settings)

private val AppScreen.icon: ImageVector
    get() = when (this) {
        AppScreen.Assessment -> Icons.Rounded.CheckCircle
        AppScreen.Status -> Icons.Rounded.Favorite
        AppScreen.Settings -> Icons.Rounded.Settings
    }

@Composable
private fun AppBottomNavigation(
    selectedScreen: AppScreen,
    onSelect: (AppScreen) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(MaterialTheme.shapes.extraLarge),
        ) {
            appScreens.forEach { screen ->
                NavigationBarItem(
                    selected = selectedScreen == screen,
                    onClick = { onSelect(screen) },
                    icon = { Icon(screen.icon, contentDescription = null) },
                    label = {
                        Text(
                            text = stringResource(screen.titleRes),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AppNavigationRail(
    selectedScreen: AppScreen,
    onSelect: (AppScreen) -> Unit,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .padding(start = 14.dp, top = 16.dp, bottom = 16.dp)
            .clip(MaterialTheme.shapes.extraLarge),
    ) {
        Spacer(Modifier.weight(1f))
        appScreens.forEach { screen ->
            NavigationRailItem(
                selected = selectedScreen == screen,
                onClick = { onSelect(screen) },
                icon = { Icon(screen.icon, contentDescription = null) },
                label = {
                    Text(
                        text = stringResource(screen.titleRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun LocalizedResources(languageTag: String, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current
    val localizedContext = remember(baseContext, baseConfiguration, languageTag) {
        val locale = Locale.forLanguageTag(languageTag)
        val configuration = Configuration(baseConfiguration)
        configuration.setLocales(LocaleList(locale))
        baseContext.createConfigurationContext(configuration)
    }
    val localizedContent: @Composable () -> Unit = {
        key(languageTag) {
            content()
        }
    }
    if (activityResultRegistryOwner == null) {
        CompositionLocalProvider(LocalContext provides localizedContext) {
            localizedContent()
        }
    } else {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
        ) {
            localizedContent()
        }
    }
}

@Composable
private fun AssessmentScreen(
    uiState: NeuroTrackUiState,
    latestSubmission: AssessmentSubmissionDisplay?,
    onSubmit: (List<Int>) -> Unit,
    onDismissResult: () -> Unit,
) {
    val questions = stringArrayResource(R.array.assessment_questions).toList()
    val options = stringArrayResource(R.array.likert_options).toList()
    var answers by rememberSaveable(questions.size) {
        mutableStateOf(List(questions.size) { -1 })
    }
    var currentQuestion by rememberSaveable(questions.size) { mutableIntStateOf(0) }
    val answeredCount = answers.count { it >= 0 }
    val progress = answeredCount / questions.size.toFloat()

    LaunchedEffect(latestSubmission?.id) {
        if (latestSubmission != null) {
            answers = List(questions.size) { -1 }
            currentQuestion = 0
        }
    }

    latestSubmission?.let { record ->
        ResultDialog(
            totalScore = record.totalScore,
            onDismiss = onDismissResult,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 840.dp)
                .align(Alignment.TopCenter),
        ) {
            item {
                PageHeader(
                    title = stringResource(R.string.assessment_title),
                    subtitle = stringResource(R.string.assessment_subtitle),
                    icon = Icons.Rounded.CheckCircle,
                )
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(18.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.assessment_progress, answeredCount, questions.size),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(
                                    R.string.assessment_percent_format,
                                    (progress * 100).roundToInt(),
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        val progressColor = MaterialTheme.colorScheme.primary
                        val progressTrack = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        ) {
                            val radius = size.height / 2f
                            drawRoundRect(
                                color = progressTrack,
                                cornerRadius = CornerRadius(radius, radius),
                            )
                            if (progress > 0f) {
                                drawRoundRect(
                                    color = progressColor,
                                    size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
                                    cornerRadius = CornerRadius(radius, radius),
                                )
                            }
                        }
                    }
                }
            }

            item {
                AnimatedContent(
                    targetState = currentQuestion,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(100))
                    },
                    label = "assessment-question",
                ) { questionIndex ->
                    AssessmentQuestionCard(
                        index = questionIndex,
                        question = questions[questionIndex],
                        options = options,
                        selected = answers[questionIndex],
                        onSelect = { optionIndex ->
                            answers = replaceAssessmentAnswer(
                                answers = answers,
                                questionIndex = questionIndex,
                                optionIndex = optionIndex,
                            )
                        },
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = { currentQuestion = (currentQuestion - 1).coerceAtLeast(0) },
                        enabled = currentQuestion > 0,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.assessment_previous))
                    }
                    if (currentQuestion < questions.lastIndex) {
                        Button(
                            onClick = { currentQuestion += 1 },
                            enabled = canAdvanceAssessment(answers, currentQuestion),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.assessment_next))
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                        }
                    } else {
                        Button(
                            onClick = { onSubmit(answers) },
                            enabled = canSubmitAssessment(answers),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.assessment_submit))
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionTitle(
                    text = stringResource(R.string.assessment_history_title),
                    supportingText = stringResource(R.string.assessment_history_subtitle),
                )
            }

            if (uiState.assessments.isEmpty()) {
                item {
                    EmptyStateCard(
                        text = stringResource(R.string.assessment_history_empty),
                        icon = Icons.Rounded.Schedule,
                    )
                }
            } else {
                itemsIndexed(uiState.assessments, key = { _, item -> item.id }) { index, record ->
                    RecordRow(
                        index = index,
                        title = stringResource(
                            R.string.assessment_history_item,
                            formatDisplayDateTime(record.createdAtMillis),
                            record.totalScore,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssessmentQuestionCard(
    index: Int,
    question: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(18.dp),
        ) {
            Text(
                text = stringResource(R.string.assessment_question_number, index + 1),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = question,
                style = MaterialTheme.typography.headlineSmall,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { optionIndex, option ->
                    val isSelected = selected == optionIndex
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        border = if (isSelected) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .selectable(
                                selected = isSelected,
                                onClick = { onSelect(optionIndex) },
                                role = Role.RadioButton,
                            ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultDialog(totalScore: Int, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                }
            }
        },
        title = { Text(stringResource(R.string.assessment_result_title, totalScore)) },
        text = {
            Text(
                when {
                    totalScore < 10 -> stringResource(R.string.assessment_feedback_low)
                    totalScore < 21 -> stringResource(R.string.assessment_feedback_medium)
                    else -> stringResource(R.string.assessment_feedback_high)
                },
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.assessment_result_close))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    )
}

@Composable
private fun StatusScreen(
    status: StatusDisplayModel,
    onStartAssessment: () -> Unit,
) {
    val stress = status.stress
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 840.dp)
                .align(Alignment.TopCenter),
        ) {
            item {
                PageHeader(
                    title = stringResource(R.string.status_title),
                    subtitle = stringResource(R.string.status_subtitle),
                    icon = Icons.Rounded.Favorite,
                )
            }

            item {
                if (stress.score == null) {
                    NoDataCard(onStartAssessment)
                } else {
                    StressCard(stress)
                }
            }

            item {
                InsightsCard(insights = status.insights)
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionTitle(
                    text = stringResource(R.string.status_sleep_section),
                    supportingText = stringResource(R.string.status_sleep_section_subtitle),
                )
            }

            item {
                LatestSleepStatusCard(summary = status.latestSleep)
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionTitle(
                    text = stringResource(R.string.status_trends_section),
                    supportingText = stringResource(R.string.status_trends_section_subtitle),
                )
            }

            item {
                StressTrendCard(trend = status.pressureTrend)
            }

            item {
                SleepStatusPeriodCard(
                    title = stringResource(R.string.status_sleep_week),
                    period = status.weekSleep,
                    days = 7,
                )
            }

            item {
                SleepStatusPeriodCard(
                    title = stringResource(R.string.status_sleep_month),
                    period = status.monthSleep,
                    days = 30,
                )
            }
        }
    }
}

@Composable
private fun NoDataCard(onStartAssessment: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null)
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.status_no_data),
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(onClick = onStartAssessment) {
                    Text(stringResource(R.string.status_go_assessment))
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun StressCard(stress: StressResult) {
    val score = stress.score ?: return
    val color = stressColor(stress.band)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
                        ),
                    ),
                )
                .padding(22.dp),
        ) {
            StressGauge(
                score = score,
                color = color,
                contentColor = stressBandContentColor(stress.band),
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.status_stress_level),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f),
                )
                Surface(
                    color = color,
                    contentColor = stressBandContentColor(stress.band),
                    shape = CircleShape,
                )
                {
                    Text(
                        text = stressBandLabel(stress.band),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                Text(
                    text = when (stress.band) {
                        StressBand.LOW -> stringResource(R.string.status_stress_low)
                        StressBand.MEDIUM -> stringResource(R.string.status_stress_medium)
                        StressBand.HIGH -> stringResource(R.string.status_stress_high)
                        null -> stringResource(R.string.status_no_data)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                )
            }
        }
    }
}

@Composable
private fun StressGauge(
    score: Double,
    color: Color,
    contentColor: Color,
) {
    val track = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(108.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            drawArc(
                color = track,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = 140f,
                sweepAngle = (score / 10.0 * 260.0).toFloat(),
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
        }
        Surface(
            color = color,
            contentColor = contentColor,
            shape = CircleShape,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.status_score_format, score),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun StressTrendCard(trend: PressureTrendDisplay) {
    val points = trend.points
    val latestScore = trend.latestScore
    val color = stressColor(trend.latestBand)

    ChartCard(title = stringResource(R.string.status_pressure_month), icon = Icons.Rounded.Favorite) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = latestScore?.let { stringResource(R.string.status_score_format, it) }
                    ?: stringResource(R.string.status_missing),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
            Text(
                text = stringResource(R.string.status_pressure_scale_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (points.none { it.score != null }) {
            EmptyText(stringResource(R.string.chart_empty))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StressTrendLineChart(points = points, lineColor = color)
                SleepChartLabels(labels = trend.chartLabels)
            }
        }
    }
}

@Composable
private fun StressTrendLineChart(
    points: List<StressTrendPoint>,
    lineColor: Color,
) {
    val guide = MaterialTheme.colorScheme.outlineVariant
    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    val halo = MaterialTheme.colorScheme.surfaceContainerLow
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp),
    ) {
        if (points.isEmpty()) return@Canvas

        val topPadding = 10.dp.toPx()
        val bottomPadding = 12.dp.toPx()
        val chartHeight = size.height - topPadding - bottomPadding
        if (chartHeight <= 0f) return@Canvas

        val bottom = topPadding + chartHeight
        val chartPoints = points.mapIndexedNotNull { index, point ->
            val score = point.score ?: return@mapIndexedNotNull null
            val x = if (points.size == 1) {
                size.width / 2f
            } else {
                index / (points.size - 1).toFloat() * size.width
            }
            val y = topPadding + (1f - (score.toFloat() / 10f).coerceIn(0f, 1f)) * chartHeight
            Offset(x, y)
        }
        if (chartPoints.isEmpty()) return@Canvas

        drawRoundRect(
            color = track,
            topLeft = Offset(0f, topPadding),
            size = Size(size.width, chartHeight),
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
        )

        listOf(0f, 0.5f, 1f).forEach { fraction ->
            val y = topPadding + fraction * chartHeight
            drawLine(
                color = guide,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        if (chartPoints.size > 1) {
            val linePath = smoothPath(chartPoints)
            val areaPath = smoothPath(chartPoints).apply {
                lineTo(chartPoints.last().x, bottom)
                lineTo(chartPoints.first().x, bottom)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.24f),
                        lineColor.copy(alpha = 0.03f),
                    ),
                    startY = topPadding,
                    endY = bottom,
                ),
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        chartPoints.forEach { point ->
            drawCircle(
                color = lineColor.copy(alpha = 0.36f),
                radius = 2.dp.toPx(),
                center = point,
            )
        }
        drawCircle(
            color = halo,
            radius = 7.dp.toPx(),
            center = chartPoints.last(),
        )
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = chartPoints.last(),
        )
    }
}

private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path

    path.moveTo(points.first().x, points.first().y)
    if (points.size == 1) return path

    for (index in 0 until points.lastIndex) {
        val p0 = points.getOrElse(index - 1) { points[index] }
        val p1 = points[index]
        val p2 = points[index + 1]
        val p3 = points.getOrElse(index + 2) { p2 }
        val control1 = Offset(
            x = p1.x + (p2.x - p0.x) / 6f,
            y = p1.y + (p2.y - p0.y) / 6f,
        )
        val control2 = Offset(
            x = p2.x - (p3.x - p1.x) / 6f,
            y = p2.y - (p3.y - p1.y) / 6f,
        )

        path.cubicTo(
            control1.x,
            control1.y,
            control2.x,
            control2.y,
            p2.x,
            p2.y,
        )
    }
    return path
}

@Composable
private fun LatestSleepStatusCard(summary: SleepSummaryDisplay) {
    ChartCard(title = stringResource(R.string.status_sleep_latest), icon = Icons.Rounded.Schedule) {
        SleepMetricRow(
            duration = summary.durationHours?.let { stringResource(R.string.status_hours_format, it) }
                ?: stringResource(R.string.status_missing),
            bedtime = summary.bedtimeText ?: stringResource(R.string.status_missing),
            wakeTime = summary.wakeTimeText ?: stringResource(R.string.status_missing),
        )
        if (!summary.hasData) {
            EmptyText(stringResource(R.string.chart_empty))
        }
    }
}

@Composable
private fun SleepStatusPeriodCard(
    title: String,
    period: SleepPeriodDisplay,
    days: Int,
) {
    ChartCard(title, icon = Icons.Rounded.Schedule) {
        SleepMetricRow(
            duration = period.durationHours?.let { stringResource(R.string.status_hours_format, it) }
                ?: stringResource(R.string.status_missing),
            bedtime = period.bedtimeText ?: stringResource(R.string.status_missing),
            wakeTime = period.wakeTimeText ?: stringResource(R.string.status_missing),
        )
        if (!period.hasData) {
            EmptyText(stringResource(R.string.chart_empty))
        } else if (days <= 7) {
            SleepDailyDurationBars(period = period)
        } else {
            SleepMonthlyAverageBars(period = period)
        }
    }
}

@Composable
private fun SleepMetricRow(
    duration: String,
    bedtime: String,
    wakeTime: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        SleepMetricColumn(
            label = stringResource(R.string.status_sleep_duration),
            value = duration,
            modifier = Modifier.weight(1f),
        )
        SleepMetricColumn(
            label = stringResource(R.string.status_bedtime),
            value = bedtime,
            modifier = Modifier.weight(1f),
        )
        SleepMetricColumn(
            label = stringResource(R.string.status_wake_time),
            value = wakeTime,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SleepMetricColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SleepDailyDurationBars(period: SleepPeriodDisplay) {
    val weekdays = stringArrayResource(R.array.weekdays).toList()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SleepDurationBars(points = period.points)
        SleepChartLabels(
            labels = period.chartLabels.map { it.text(weekdays) },
        )
    }
}

@Composable
private fun SleepMonthlyAverageBars(period: SleepPeriodDisplay) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SleepDurationBars(points = period.points)
        SleepChartLabels(labels = period.chartLabels.map { it.text(emptyList()) })
    }
}

private fun SleepChartLabel.text(weekdays: List<String>): String =
    when (this) {
        is SleepChartLabel.Weekday -> weekdays[dayOfWeekValue.coerceIn(1, 7) - 1]
        is SleepChartLabel.Text -> value
    }

@Composable
private fun SleepDurationBars(points: List<SleepDurationPoint>) {
    val primary = MaterialTheme.colorScheme.primary
    val shortSleep = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    val guide = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp),
    ) {
        if (points.isEmpty()) return@Canvas

        val maxMinutes = maxOf(600f, points.mapNotNull { it.durationMinutes }.maxOrNull() ?: 0f)
        val topPadding = 6.dp.toPx()
        val bottomPadding = 4.dp.toPx()
        val chartHeight = size.height - topPadding - bottomPadding
        if (chartHeight <= 0f) return@Canvas

        val bottom = topPadding + chartHeight
        val slotWidth = size.width / points.size
        val barWidth = minOf(slotWidth * 0.42f, 26.dp.toPx()).coerceAtLeast(5.dp.toPx())
        val targetY = bottom - (480f / maxMinutes).coerceIn(0f, 1f) * chartHeight

        drawLine(
            color = guide,
            start = Offset(0f, targetY),
            end = Offset(size.width, targetY),
            strokeWidth = 1.dp.toPx(),
        )

        points.forEachIndexed { index, point ->
            val left = index * slotWidth + (slotWidth - barWidth) / 2
            val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
            drawRoundRect(
                color = track,
                topLeft = Offset(left, topPadding),
                size = Size(barWidth, chartHeight),
                cornerRadius = corner,
            )
            point.durationMinutes?.let { minutes ->
                val barHeight = ((minutes / maxMinutes).coerceIn(0f, 1f) * chartHeight)
                    .coerceAtLeast(4.dp.toPx())
                drawRoundRect(
                    color = if (minutes < 360f) shortSleep else primary,
                    topLeft = Offset(left, bottom - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = corner,
                )
            }
        }
    }
}

@Composable
private fun SleepChartLabels(labels: List<String>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun InsightsCard(insights: List<StatusInsight>) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(18.dp),
        ) {
            Text(
                text = stringResource(R.string.status_insights),
                style = MaterialTheme.typography.titleMedium,
            )
            insights.forEach { insight ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.secondary,
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                                modifier = Modifier.size(16.dp),
                        )
                        }
                    }
                    Text(
                        text = stringResource(insight.stringRes),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: com.example.neurotrack.AppSettings,
    onLanguageChange: (String) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onReminderChange: (Int, Int) -> Unit,
    onExportLogs: (Context) -> Unit,
    onExportSleepRawData: (Context, Long, Long) -> Unit,
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 840.dp)
                .align(Alignment.TopCenter),
        ) {
            item {
                PageHeader(
                    title = stringResource(R.string.settings_title),
                    subtitle = stringResource(R.string.settings_subtitle),
                    icon = Icons.Rounded.Settings,
                )
            }
            item {
                LanguageSection(settings.languageTag, onLanguageChange)
            }
            item {
                ThemeSection(settings.themeMode, onThemeModeChange)
            }
            item {
                ReminderSection(settings, onReminderChange)
            }
            item {
                PermissionSection()
            }
            item {
                ExportsSection(
                    onExportLogs = { onExportLogs(context) },
                    onExportSleepRawData = { startMillis, endMillis ->
                        onExportSleepRawData(context, startMillis, endMillis)
                    },
                )
            }
            item {
                AboutSection()
            }
        }
    }
}

@Composable
private fun PermissionSection() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshTick += 1
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshTick += 1
    }
    val notificationGranted = remember(refreshTick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val locationGranted = remember(refreshTick) {
        LocationSleepSignalReader.hasLocationPermission(context)
    }
    val usageStatsGranted = remember(refreshTick) {
        PermissionIntents.hasUsageStatsAccess(context)
    }
    val powerManager = context.getSystemService(PowerManager::class.java)
    val batteryGranted = remember(refreshTick) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    val exactAlarmGranted = remember(refreshTick) {
        PermissionIntents.canScheduleExactAlarms(context)
    }

    SectionCard(
        title = stringResource(R.string.settings_permissions),
        icon = Icons.Rounded.Info,
        action = {
            IconButton(
                onClick = { refreshTick += 1 },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.permission_refresh),
                    modifier = Modifier.size(20.dp),
                )
            }
        },
    ) {
        Text(
            text = stringResource(R.string.permission_usage_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PermissionRow(
            title = stringResource(R.string.permission_usage_stats),
            subtitle = stringResource(R.string.permission_usage_stats_desc),
            granted = usageStatsGranted,
            icon = Icons.Rounded.Info,
            onClick = {
                openIntent(context, PermissionIntents.usageAccessSettings())
                refreshTick += 1
            },
        )
        PermissionRow(
            title = stringResource(R.string.permission_notifications),
            subtitle = stringResource(R.string.permission_notifications_desc),
            granted = notificationGranted,
            icon = Icons.Rounded.Notifications,
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationGranted) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openIntent(context, PermissionIntents.notificationSettings(context))
                }
            },
        )
        PermissionRow(
            title = stringResource(R.string.permission_location),
            subtitle = stringResource(R.string.permission_location_desc),
            granted = locationGranted,
            icon = Icons.Rounded.Info,
            onClick = {
                if (!locationGranted) {
                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                } else {
                    openIntent(context, PermissionIntents.appSettings(context))
                }
                refreshTick += 1
            },
        )
        PermissionRow(
            title = stringResource(R.string.permission_battery),
            subtitle = stringResource(R.string.permission_battery_desc),
            granted = batteryGranted,
            icon = Icons.Rounded.BatterySaver,
            onClick = {
                openIntent(context, PermissionIntents.batteryOptimizationSettings(context))
                refreshTick += 1
            },
        )
        PermissionRow(
            title = stringResource(R.string.permission_exact_alarm),
            subtitle = stringResource(R.string.permission_exact_alarm_desc),
            granted = exactAlarmGranted,
            icon = Icons.Rounded.Schedule,
            onClick = {
                openIntent(context, PermissionIntents.exactAlarmSettings(context))
                refreshTick += 1
            },
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                contentDescription = null,
                tint = if (granted) semanticSuccess() else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = if (granted) {
                    stringResource(R.string.permission_granted)
                } else {
                    stringResource(R.string.permission_missing)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (granted) semanticSuccess() else MaterialTheme.colorScheme.tertiary,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun LanguageSection(languageTag: String, onLanguageChange: (String) -> Unit) {
    SectionCard(title = stringResource(R.string.settings_language), icon = Icons.Rounded.Language) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
        ) {
            ChoicePill(
                selected = languageTag == SettingsStore.LANGUAGE_ZH,
                onClick = { onLanguageChange(SettingsStore.LANGUAGE_ZH) },
                text = stringResource(R.string.language_zh),
                modifier = Modifier.weight(1f),
            )
            ChoicePill(
                selected = languageTag == SettingsStore.LANGUAGE_EN,
                onClick = { onLanguageChange(SettingsStore.LANGUAGE_EN) },
                text = stringResource(R.string.language_en),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeSection(themeMode: String, onThemeModeChange: (String) -> Unit) {
    SectionCard(title = stringResource(R.string.settings_theme), icon = Icons.Rounded.Settings) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
        ) {
            ChoicePill(
                selected = themeMode == SettingsStore.THEME_SYSTEM,
                onClick = { onThemeModeChange(SettingsStore.THEME_SYSTEM) },
                text = stringResource(R.string.theme_system),
                modifier = Modifier.weight(1f),
            )
            ChoicePill(
                selected = themeMode == SettingsStore.THEME_LIGHT,
                onClick = { onThemeModeChange(SettingsStore.THEME_LIGHT) },
                text = stringResource(R.string.theme_light),
                modifier = Modifier.weight(1f),
            )
            ChoicePill(
                selected = themeMode == SettingsStore.THEME_DARK,
                onClick = { onThemeModeChange(SettingsStore.THEME_DARK) },
                text = stringResource(R.string.theme_dark),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChoicePill(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = CircleShape,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .heightIn(min = 46.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReminderSection(
    settings: com.example.neurotrack.AppSettings,
    onReminderChange: (Int, Int) -> Unit,
) {
    val weekdays = stringArrayResource(R.array.weekdays).toList()
    SectionCard(title = stringResource(R.string.settings_reminder), icon = Icons.Rounded.Notifications) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DropdownSelector(
                label = stringResource(R.string.reminder_day),
                value = weekdays[settings.reminderDayOfWeek.coerceIn(1, 7) - 1],
                options = weekdays,
                modifier = Modifier.weight(1f),
                onSelect = { index -> onReminderChange(index + 1, settings.reminderHour) },
            )
            val hourOptions = (0..23).map { "%02d:00".format(it) }
            DropdownSelector(
                label = stringResource(R.string.reminder_hour),
                value = hourOptions[settings.reminderHour.coerceIn(0, 23)],
                options = hourOptions,
                modifier = Modifier.weight(1f),
                onSelect = { index -> onReminderChange(settings.reminderDayOfWeek, index) },
            )
        }
        Text(
            text = stringResource(R.string.reminder_rule),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DropdownSelector(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                )
            }
        }
    }
}

@Composable
private fun ExportsSection(
    onExportLogs: () -> Unit,
    onExportSleepRawData: (Long, Long) -> Unit,
) {
    val defaultRange = remember { SleepRawExportRangeModel.defaultRange() }
    var startText by rememberSaveable { mutableStateOf(SleepRawExportRangeModel.format(defaultRange.startMillis)) }
    var endText by rememberSaveable { mutableStateOf(SleepRawExportRangeModel.format(defaultRange.endMillis)) }
    val startMillis = remember(startText) { SleepRawExportRangeModel.parse(startText) }
    val endMillis = remember(endText) { SleepRawExportRangeModel.parse(endText) }
    val canExportSleepRawData = startMillis != null && endMillis != null && endMillis > startMillis

    SectionCard(title = stringResource(R.string.settings_exports), icon = Icons.Rounded.Download) {
        OutlinedButton(onClick = onExportLogs, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.export_logs))
        }
        HorizontalDivider()
        OutlinedTextField(
            value = startText,
            onValueChange = { startText = it },
            label = { Text(stringResource(R.string.export_range_start)) },
            singleLine = true,
            isError = startText.isNotBlank() && startMillis == null,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = endText,
            onValueChange = { endText = it },
            label = { Text(stringResource(R.string.export_range_end)) },
            singleLine = true,
            isError = endText.isNotBlank() && endMillis == null,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!canExportSleepRawData) {
            Text(
                text = stringResource(R.string.export_range_invalid),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                val start = startMillis
                val end = endMillis
                if (start != null && end != null && end > start) {
                    onExportSleepRawData(start, end)
                }
            },
            enabled = canExportSleepRawData,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.export_sleep_raw_data))
        }
    }
}

private val StatusInsight.stringRes: Int
    get() = when (this) {
        StatusInsight.NO_SLEEP -> R.string.status_insight_no_sleep
        StatusInsight.LATER -> R.string.status_insight_later
        StatusInsight.IRREGULAR -> R.string.status_insight_irregular
        StatusInsight.SHORT -> R.string.status_insight_short
        StatusInsight.DROP -> R.string.status_insight_drop
        StatusInsight.LATE_AVERAGE -> R.string.status_insight_late_average
        StatusInsight.STABLE -> R.string.status_insight_stable
    }

@Composable
private fun AboutSection() {
    SectionCard(title = stringResource(R.string.settings_about), icon = Icons.Rounded.Info) {
        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
        Text(stringResource(R.string.about_license))
        Text(stringResource(R.string.about_project))
        HorizontalDivider()
        Text(
            text = stringResource(R.string.disclaimer_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.disclaimer_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Favorite,
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionCard(title = title, icon = icon, content = content)
}

@Composable
private fun PageHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = stringResource(R.string.app_name).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String, supportingText: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun RecordRow(index: Int, title: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Surface(
                color = if (index == 0) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                contentColor = if (index == 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                shape = CircleShape,
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(19.dp))
                }
            }
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EmptyStateCard(text: String, icon: ImageVector) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(28.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun stressColor(band: StressBand?): Color =
    when (band) {
        StressBand.LOW -> semanticSuccess()
        StressBand.MEDIUM -> MaterialTheme.colorScheme.tertiary
        StressBand.HIGH -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.primary
    }

@Composable
private fun semanticSuccess(): Color = MaterialTheme.colorScheme.secondary

@Composable
private fun stressBandLabel(band: StressBand?): String =
    when (band) {
        StressBand.LOW -> stringResource(R.string.status_band_low)
        StressBand.MEDIUM -> stringResource(R.string.status_band_medium)
        StressBand.HIGH -> stringResource(R.string.status_band_high)
        null -> stringResource(R.string.status_missing)
    }

@Composable
private fun stressBandContentColor(band: StressBand?): Color =
    when (band) {
        StressBand.LOW -> MaterialTheme.colorScheme.onSecondary
        StressBand.MEDIUM -> MaterialTheme.colorScheme.onTertiary
        StressBand.HIGH -> MaterialTheme.colorScheme.onError
        null -> MaterialTheme.colorScheme.onPrimary
    }

private fun openIntent(context: Context, intent: Intent) {
    runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
