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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.neurotrack.BuildConfig
import com.example.neurotrack.R
import com.example.neurotrack.SettingsStore
import com.example.neurotrack.background.LocationSleepSignalReader
import com.example.neurotrack.background.PermissionIntents
import com.example.neurotrack.data.AssessmentRecordEntity
import com.example.neurotrack.data.SleepRecordEntity
import com.example.neurotrack.domain.SleepPenaltyMetrics
import com.example.neurotrack.domain.StressBand
import com.example.neurotrack.domain.StressCalculator
import com.example.neurotrack.domain.StressResult
import com.example.neurotrack.domain.StressTrendPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class AppScreen(val titleRes: Int) {
    Assessment(R.string.nav_assessment),
    Status(R.string.nav_status),
    Settings(R.string.nav_settings),
}

fun destinationToScreen(destination: String?): AppScreen =
    if (destination == "assessment") AppScreen.Assessment else AppScreen.Status

@OptIn(ExperimentalMaterial3Api::class)
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

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(selectedScreen.titleRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf(AppScreen.Assessment, AppScreen.Status, AppScreen.Settings).forEach { screen ->
                        NavigationBarItem(
                            selected = selectedScreen == screen,
                            onClick = { selectedScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        AppScreen.Assessment -> Icons.Rounded.CheckCircle
                                        AppScreen.Status -> Icons.Rounded.Favorite
                                        AppScreen.Settings -> Icons.Rounded.Settings
                                    },
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(screen.titleRes)) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
                },
                label = "screen",
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) { screen ->
                when (screen) {
                    AppScreen.Assessment -> AssessmentScreen(
                        uiState = uiState,
                        latestSubmission = latestSubmission,
                        onSubmit = viewModel::submitAssessment,
                        onDismissResult = viewModel::clearLatestSubmission,
                    )

                    AppScreen.Status -> StatusScreen(
                        uiState = uiState,
                        onStartAssessment = { selectedScreen = AppScreen.Assessment },
                    )

                    AppScreen.Settings -> SettingsScreen(
                        settings = settings,
                        onLanguageChange = viewModel::setLanguage,
                        onThemeModeChange = viewModel::setThemeMode,
                        onReminderChange = viewModel::setReminder,
                        onExportLogs = viewModel::exportLogs,
                    )
                }
            }
        }
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
    latestSubmission: com.example.neurotrack.data.AssessmentRecordEntity?,
    onSubmit: (List<Int>) -> Unit,
    onDismissResult: () -> Unit,
) {
    val questions = stringArrayResource(R.array.assessment_questions).toList()
    val options = stringArrayResource(R.array.likert_options).toList()
    var answers by rememberSaveable(questions.size) {
        mutableStateOf(List(questions.size) { -1 })
    }
    val answeredCount = answers.count { it >= 0 }
    val progress = answeredCount / questions.size.toFloat()

    LaunchedEffect(latestSubmission?.id) {
        if (latestSubmission != null) {
            answers = List(questions.size) { -1 }
        }
    }

    latestSubmission?.let { record ->
        ResultDialog(
            totalScore = record.totalScore,
            onDismiss = onDismissResult,
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.assessment_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.assessment_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.assessment_progress, answeredCount, questions.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        items(questions.indices.toList()) { index ->
            AssessmentQuestionCard(
                index = index,
                question = questions[index],
                options = options,
                selected = answers[index],
                onSelect = { optionIndex ->
                    answers = answers.toMutableList().also { it[index] = optionIndex }
                },
            )
        }

        item {
            Button(
                onClick = { onSubmit(answers) },
                enabled = answers.all { it >= 0 },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.assessment_submit))
            }
        }

        item {
            SectionTitle(text = stringResource(R.string.assessment_history_title))
        }

        if (uiState.assessments.isEmpty()) {
            item {
                EmptyText(text = stringResource(R.string.assessment_history_empty))
            }
        } else {
            items(uiState.assessments, key = { it.id }) { record ->
                RecordRow(
                    title = stringResource(
                        R.string.assessment_history_item,
                        formatDateTime(record.createdAtMillis),
                        record.totalScore,
                    ),
                    subtitle = record.answersCsv,
                )
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "${index + 1}. $question",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                options.forEachIndexed { optionIndex, option ->
                    val isSelected = selected == optionIndex
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .clickable { onSelect(optionIndex) },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.assessment_result_close))
            }
        },
    )
}

@Composable
private fun StatusScreen(
    uiState: NeuroTrackUiState,
    onStartAssessment: () -> Unit,
) {
    val stress = uiState.stressResult
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            if (stress.score == null) {
                NoDataCard(onStartAssessment)
            } else {
                StressCard(stress)
            }
        }

        item {
            StressGradientBar(score = stress.score)
        }

        item {
            StressTrendCard(
                assessments = uiState.assessments,
                sleepRecords = uiState.sleepRecords,
            )
        }

        item {
            YesterdaySleepStatusCard(records = uiState.sleepRecords)
        }

        item {
            SleepStatusPeriodCard(
                title = stringResource(R.string.status_sleep_week),
                records = uiState.sleepRecords,
                days = 7,
            )
        }

        item {
            SleepStatusPeriodCard(
                title = stringResource(R.string.status_sleep_month),
                records = uiState.sleepRecords,
                days = 30,
            )
        }

        item {
            InsightsCard(stress.metrics)
        }
    }
}

@Composable
private fun NoDataCard(onStartAssessment: () -> Unit) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.status_no_data),
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onStartAssessment) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.status_go_assessment))
            }
        }
    }
}

@Composable
private fun StressCard(stress: StressResult) {
    val score = stress.score ?: return
    val color = stressColor(stress.band)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            StressGauge(score = score, color = color)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.status_stress_level),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.status_stress_format, score),
                    style = MaterialTheme.typography.displaySmall,
                    color = color,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when (stress.band) {
                        StressBand.LOW -> stringResource(R.string.status_stress_low)
                        StressBand.MEDIUM -> stringResource(R.string.status_stress_medium)
                        StressBand.HIGH -> stringResource(R.string.status_stress_high)
                        null -> stringResource(R.string.status_no_data)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun StressGauge(score: Double, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(112.dp),
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
        Text(
            text = "${(score * 10).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StressGradientBar(score: Double?) {
    ChartCard(title = stringResource(R.string.status_pressure_scale)) {
        val markerColor = score?.let {
            when {
                it < 4.0 -> stressColor(StressBand.LOW)
                it < 7.0 -> stressColor(StressBand.MEDIUM)
                else -> stressColor(StressBand.HIGH)
            }
        } ?: MaterialTheme.colorScheme.outline
        val scoreText = score?.let { stringResource(R.string.status_score_format, it) }
            ?: stringResource(R.string.status_missing)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = scoreText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = markerColor,
            )
            Text(
                text = stringResource(R.string.status_pressure_scale_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
        ) {
            val barHeight = 18.dp.toPx()
            val top = (size.height - barHeight) / 2f
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF2E7D5B),
                        Color(0xFFE0A82E),
                        Color(0xFFBA1A1A),
                    ),
                ),
                topLeft = Offset(0f, top),
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f),
            )
            score?.let {
                val x = (it.coerceIn(0.0, 10.0) / 10.0 * size.width).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = 10.dp.toPx(),
                    center = Offset(x, size.height / 2f),
                )
                drawCircle(
                    color = markerColor,
                    radius = 7.dp.toPx(),
                    center = Offset(x, size.height / 2f),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("0", style = MaterialTheme.typography.labelSmall)
            Text("5", style = MaterialTheme.typography.labelSmall)
            Text("10", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun StressTrendCard(
    assessments: List<AssessmentRecordEntity>,
    sleepRecords: List<SleepRecordEntity>,
) {
    val today = LocalDate.now()
    val points = remember(assessments, sleepRecords, today) {
        StressCalculator.trendPoints(
            assessments = assessments,
            sleepRecords = sleepRecords,
            endDate = today,
            days = 30,
        )
    }
    val latestScore = points.lastOrNull { it.score != null }?.score
    val color = stressColor(stressBandForScore(latestScore))

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
                SleepChartLabels(
                    labels = listOf(
                        formatMonthDay(points.first().date),
                        formatMonthDay(points[points.size / 2].date),
                        formatMonthDay(points.last().date),
                    ),
                )
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
private fun YesterdaySleepStatusCard(records: List<SleepRecordEntity>) {
    val today = LocalDate.now().toEpochDay()
    val record = remember(records, today) {
        latestSleepRecordForDisplay(records, today)
    }

    ChartCard(title = stringResource(R.string.status_sleep_yesterday), icon = Icons.Rounded.Schedule) {
        SleepMetricRow(
            duration = record?.let { stringResource(R.string.status_hours_format, it.durationMinutes / 60.0) }
                ?: stringResource(R.string.status_missing),
            bedtime = record?.let { formatClockTime(it.sleepStartMillis) }
                ?: stringResource(R.string.status_missing),
            wakeTime = record?.let { formatClockTime(it.sleepEndMillis) }
                ?: stringResource(R.string.status_missing),
        )
        if (record == null) {
            EmptyText(stringResource(R.string.chart_empty))
        }
    }
}

internal fun latestSleepRecordForDisplay(
    records: List<SleepRecordEntity>,
    todayEpochDay: Long = LocalDate.now().toEpochDay(),
): SleepRecordEntity? =
    records
        .asSequence()
        .filter { !it.isMissing && it.durationMinutes > 0 && it.dateEpochDay <= todayEpochDay }
        .maxByOrNull { it.dateEpochDay }

@Composable
private fun SleepStatusPeriodCard(
    title: String,
    records: List<SleepRecordEntity>,
    days: Int,
) {
    val today = LocalDate.now()
    val data = remember(records, days, today) {
        sleepRecordsForPeriod(records, today, days)
    }

    ChartCard(title, icon = Icons.Rounded.Schedule) {
        SleepMetricRow(
            duration = if (data.isEmpty()) {
                stringResource(R.string.status_missing)
            } else {
                stringResource(R.string.status_hours_format, data.map { it.durationMinutes }.average() / 60.0)
            },
            bedtime = averageClockText(data.map { nightMinute(it.sleepStartMillis) })
                ?: stringResource(R.string.status_missing),
            wakeTime = averageClockText(data.map { minuteOfDay(it.sleepEndMillis).toFloat() })
                ?: stringResource(R.string.status_missing),
        )
        if (data.isEmpty()) {
            EmptyText(stringResource(R.string.chart_empty))
        } else if (days <= 7) {
            SleepDailyDurationBars(records = data, today = today)
        } else {
            SleepMonthlyAverageBars(records = data, today = today)
        }
    }
}

internal fun sleepRecordsForPeriod(
    records: List<SleepRecordEntity>,
    today: LocalDate = LocalDate.now(),
    days: Int,
): List<SleepRecordEntity> {
    val endEpochDay = today.toEpochDay()
    val startEpochDay = today.minusDays((days - 1).toLong()).toEpochDay()
    return records
        .filter { !it.isMissing && it.durationMinutes > 0 }
        .filter { it.dateEpochDay in startEpochDay..endEpochDay }
        .sortedBy { it.dateEpochDay }
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
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class SleepDurationPoint(
    val date: LocalDate,
    val durationMinutes: Float?,
)

@Composable
private fun SleepDailyDurationBars(
    records: List<SleepRecordEntity>,
    today: LocalDate,
) {
    val weekdays = stringArrayResource(R.array.weekdays).toList()
    val points = remember(records, today) {
        dailySleepDurationPoints(records = records, today = today, days = 7)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SleepDurationBars(points = points)
        SleepChartLabels(
            labels = points.map { point ->
                weekdays[point.date.dayOfWeek.value - 1]
            },
        )
    }
}

@Composable
private fun SleepMonthlyAverageBars(
    records: List<SleepRecordEntity>,
    today: LocalDate,
) {
    val points = remember(records, today) {
        monthlySleepAveragePoints(records = records, today = today)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SleepDurationBars(points = points)
        SleepChartLabels(labels = points.map { formatMonthDay(it.date) })
    }
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

private fun dailySleepDurationPoints(
    records: List<SleepRecordEntity>,
    today: LocalDate,
    days: Int,
): List<SleepDurationPoint> {
    val startDate = today.minusDays((days - 1).toLong())
    val recordsByDay = records.associateBy { it.dateEpochDay }
    return (0 until days).map { offset ->
        val date = startDate.plusDays(offset.toLong())
        SleepDurationPoint(
            date = date,
            durationMinutes = recordsByDay[date.toEpochDay()]?.durationMinutes?.toFloat(),
        )
    }
}

private fun monthlySleepAveragePoints(
    records: List<SleepRecordEntity>,
    today: LocalDate,
): List<SleepDurationPoint> {
    val startDate = today.minusDays(29)
    return (0 until 5).map { bucketIndex ->
        val bucketStart = startDate.plusDays((bucketIndex * 7).toLong())
        val bucketEnd = bucketStart.plusDays(6).let { end ->
            if (end.isAfter(today)) today else end
        }
        val startEpochDay = bucketStart.toEpochDay()
        val endEpochDay = bucketEnd.toEpochDay()
        val durations = records
            .filter { it.dateEpochDay in startEpochDay..endEpochDay }
            .map { it.durationMinutes }
        SleepDurationPoint(
            date = bucketStart,
            durationMinutes = durations.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
        )
    }
}

@Composable
private fun MetricGrid(stress: StressResult) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard(
            label = stringResource(R.string.status_latest_assessment),
            value = stress.latestAssessmentScore?.let { stringResource(R.string.status_score_format, it) }
                ?: stringResource(R.string.status_missing),
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = stringResource(R.string.status_trend_assessment),
            value = stress.trendAssessmentScore?.let { stringResource(R.string.status_score_format, it) }
                ?: stringResource(R.string.status_missing),
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = stringResource(R.string.status_sleep_penalty),
            value = stress.sleepPenaltyScore?.let { stringResource(R.string.status_score_format, it) }
                ?: stringResource(R.string.status_missing),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DurationBarChart(title: String, records: List<SleepRecordEntity>, days: Int) {
    ChartCard(title, icon = Icons.Rounded.Schedule) {
        val data = records
            .filter { !it.isMissing }
            .sortedBy { it.dateEpochDay }
            .takeLast(days)
        if (data.isEmpty()) {
            EmptyText(stringResource(R.string.chart_empty))
        } else {
            val color = MaterialTheme.colorScheme.primary
            val track = MaterialTheme.colorScheme.surfaceVariant
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            ) {
                val maxMinutes = maxOf(480f, data.maxOf { it.durationMinutes }.toFloat())
                val slotWidth = size.width / data.size
                data.forEachIndexed { index, record ->
                    val height = (record.durationMinutes / maxMinutes) * size.height
                    val barWidth = slotWidth * 0.46f
                    val left = index * slotWidth + (slotWidth - barWidth) / 2
                    drawRoundRect(
                        color = track,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(12f, 12f),
                    )
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(12f, 12f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyAverageChart(title: String, records: List<SleepRecordEntity>) {
    ChartCard(title, icon = Icons.Rounded.Schedule) {
        val today = LocalDate.now().toEpochDay()
        val values = (0..3).map { week ->
            val rangeStart = week * 7
            val rangeEnd = rangeStart + 6
            records
                .filter { !it.isMissing }
                .filter { (today - it.dateEpochDay).toInt() in rangeStart..rangeEnd }
                .map { it.durationMinutes }
                .let { if (it.isEmpty()) 0f else it.average().toFloat() }
        }.asReversed()

        if (values.all { it == 0f }) {
            EmptyText(stringResource(R.string.chart_empty))
        } else {
            val color = MaterialTheme.colorScheme.tertiary
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
            ) {
                val maxMinutes = maxOf(480f, values.maxOrNull() ?: 0f)
                val slotWidth = size.width / values.size
                values.forEachIndexed { index, minutes ->
                    val height = (minutes / maxMinutes) * size.height
                    val barWidth = slotWidth * 0.48f
                    val left = index * slotWidth + (slotWidth - barWidth) / 2
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(12f, 12f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepWindowChart(title: String, records: List<SleepRecordEntity>) {
    ChartCard(title, icon = Icons.Rounded.Schedule) {
        val data = records
            .filter { !it.isMissing }
            .sortedBy { it.dateEpochDay }
            .takeLast(7)
        if (data.isEmpty()) {
            EmptyText(stringResource(R.string.chart_empty))
        } else {
            val sleepColor = MaterialTheme.colorScheme.primary
            val wakeColor = MaterialTheme.colorScheme.secondary
            val guide = MaterialTheme.colorScheme.surfaceVariant
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
            ) {
                val rows = data.size
                val rowHeight = size.height / rows
                val startBound = 20 * 60f
                val endBound = 36 * 60f
                data.forEachIndexed { index, record ->
                    val y = rowHeight * index + rowHeight / 2
                    val start = nightMinute(record.sleepStartMillis)
                    val end = nightMinute(record.sleepEndMillis)
                    val x1 = ((start - startBound) / (endBound - startBound)).coerceIn(0f, 1f) * size.width
                    val x2 = ((end - startBound) / (endBound - startBound)).coerceIn(0f, 1f) * size.width
                    drawLine(guide, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
                    drawLine(
                        sleepColor,
                        Offset(x1, y),
                        Offset(x2.coerceAtLeast(x1 + 2f), y),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(sleepColor, radius = 7f, center = Offset(x1, y))
                    drawCircle(wakeColor, radius = 7f, center = Offset(x2, y))
                }
            }
        }
    }
}

@Composable
private fun InsightsCard(metrics: SleepPenaltyMetrics) {
    ChartCard(title = stringResource(R.string.status_insights), icon = Icons.Rounded.CheckCircle) {
        val insights = remember(metrics) {
            when {
                !metrics.hasSleepData -> listOf(R.string.status_insight_no_sleep)
                else -> buildList {
                    if (metrics.bedtimeGettingLater) add(R.string.status_insight_later)
                    if (metrics.irregularBedtime) add(R.string.status_insight_irregular)
                    if (metrics.shortSleep) add(R.string.status_insight_short)
                    if (metrics.suddenDurationDrop) add(R.string.status_insight_drop)
                    if (metrics.lateAverageBedtime) add(R.string.status_insight_late_average)
                    if (isEmpty()) add(R.string.status_insight_stable)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            insights.forEach { resId ->
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(resId)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
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
) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            PermissionSection()
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
            LogsSection(onExportLogs = { onExportLogs(context) })
        }
        item {
            AboutSection()
        }
    }
}

@Composable
private fun PermissionSection() {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
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
        AssistChip(
            onClick = onClick,
            label = {
                Text(
                    if (granted) {
                        stringResource(R.string.permission_granted)
                    } else {
                        stringResource(R.string.permission_missing)
                    },
                )
            },
        )
    }
}

@Composable
private fun LanguageSection(languageTag: String, onLanguageChange: (String) -> Unit) {
    SectionCard(title = stringResource(R.string.settings_language), icon = Icons.Rounded.Language) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = languageTag == SettingsStore.LANGUAGE_ZH,
                onClick = { onLanguageChange(SettingsStore.LANGUAGE_ZH) },
                label = { Text(stringResource(R.string.language_zh)) },
            )
            FilterChip(
                selected = languageTag == SettingsStore.LANGUAGE_EN,
                onClick = { onLanguageChange(SettingsStore.LANGUAGE_EN) },
                label = { Text(stringResource(R.string.language_en)) },
            )
        }
    }
}

@Composable
private fun ThemeSection(themeMode: String, onThemeModeChange: (String) -> Unit) {
    SectionCard(title = stringResource(R.string.settings_theme), icon = Icons.Rounded.Settings) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = themeMode == SettingsStore.THEME_SYSTEM,
                onClick = { onThemeModeChange(SettingsStore.THEME_SYSTEM) },
                label = { Text(stringResource(R.string.theme_system)) },
            )
            FilterChip(
                selected = themeMode == SettingsStore.THEME_LIGHT,
                onClick = { onThemeModeChange(SettingsStore.THEME_LIGHT) },
                label = { Text(stringResource(R.string.theme_light)) },
            )
            FilterChip(
                selected = themeMode == SettingsStore.THEME_DARK,
                onClick = { onThemeModeChange(SettingsStore.THEME_DARK) },
                label = { Text(stringResource(R.string.theme_dark)) },
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
private fun LogsSection(onExportLogs: () -> Unit) {
    SectionCard(title = stringResource(R.string.settings_logs), icon = Icons.Rounded.Download) {
        Button(onClick = onExportLogs, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.export_logs))
        }
    }
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(icon, contentDescription = null)
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
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
private fun RecordRow(title: String, subtitle: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun stressColor(band: StressBand?): Color =
    when (band) {
        StressBand.LOW -> Color(0xFF2E7D5B)
        StressBand.MEDIUM -> Color(0xFF9A6B00)
        StressBand.HIGH -> Color(0xFFBA1A1A)
        null -> MaterialTheme.colorScheme.primary
    }

private fun stressBandForScore(score: Double?): StressBand? =
    when {
        score == null -> null
        score < 4.0 -> StressBand.LOW
        score < 7.0 -> StressBand.MEDIUM
        else -> StressBand.HIGH
    }

private fun formatDateTime(millis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(formatter)
}

private fun formatMonthDay(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("M/d", Locale.getDefault()))

private fun formatClockTime(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
}

private fun nightMinute(millis: Long): Float {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    val minute = time.hour * 60 + time.minute
    return if (minute < 20 * 60) (minute + 24 * 60).toFloat() else minute.toFloat()
}

private fun minuteOfDay(millis: Long): Int {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return time.hour * 60 + time.minute
}

private fun averageClockText(minutes: List<Float>): String? {
    if (minutes.isEmpty()) return null
    val average = minutes.average().roundToInt()
    val normalized = ((average % (24 * 60)) + (24 * 60)) % (24 * 60)
    return "%02d:%02d".format(normalized / 60, normalized % 60)
}

private fun openIntent(context: Context, intent: Intent) {
    runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
