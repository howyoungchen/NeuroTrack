package com.example.neurotrack.ui

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.neurotrack.BuildConfig
import com.example.neurotrack.R
import com.example.neurotrack.SettingsStore
import com.example.neurotrack.domain.MindfulnessSessionStatus
import com.example.neurotrack.domain.MindfulnessSchedule
import com.example.neurotrack.domain.StressBand
import com.example.neurotrack.domain.WeeklyStressPoint
import java.time.LocalDate
import kotlinx.coroutines.delay
import java.util.Locale

enum class AppScreen(val titleRes: Int, val icon: ImageVector) {
    STATUS(R.string.nav_status, Icons.Rounded.Favorite),
    PRACTICE(R.string.nav_practice, Icons.Rounded.SelfImprovement),
    SETTINGS(R.string.nav_settings, Icons.Rounded.Settings),
}

fun destinationToScreen(destination: String?): AppScreen =
    if (destination == "practice") AppScreen.PRACTICE else AppScreen.STATUS

@Composable
fun NeuroTrackRoot(viewModel: NeuroTrackViewModel, initialScreen: AppScreen = AppScreen.STATUS) {
    val settings by viewModel.settings.collectAsState()
    val session by viewModel.session.collectAsState()
    LocalizedResources(settings.languageTag) {
        if (session.active) {
            MindfulnessSessionScreen(
                state = session,
                onEnd = viewModel::abandonMindfulness,
                onFocusUnavailable = viewModel::interruptMindfulness,
            )
            return@LocalizedResources
        }
        val uiState by viewModel.uiState.collectAsState()
        val latestSubmission by viewModel.latestSubmission.collectAsState()
        var selectedScreen by rememberSaveable { mutableStateOf(initialScreen) }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                ) {
                    AppScreen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = screen == selectedScreen,
                            onClick = { selectedScreen = screen },
                            icon = { Icon(screen.icon, null) },
                            label = { Text(stringResource(screen.titleRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = selectedScreen,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "destination",
                modifier = Modifier.padding(padding),
            ) { screen ->
                when (screen) {
                    AppScreen.STATUS -> StatusScreen(uiState.status)
                    AppScreen.PRACTICE -> PracticeScreen(
                        uiState = uiState,
                        session = session,
                        latestSubmission = latestSubmission,
                        onSubmit = viewModel::submitAssessment,
                        onDismissSubmission = viewModel::clearLatestSubmission,
                        onStartMindfulness = viewModel::startMindfulness,
                        onClearSessionResult = viewModel::clearSessionResult,
                    )
                    AppScreen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onLanguage = viewModel::setLanguage,
                        onTheme = viewModel::setThemeMode,
                        onReminder = viewModel::setReminderTime,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalizedResources(languageTag: String, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val configuration = LocalConfiguration.current
    val localizedContext = remember(baseContext, configuration, languageTag) {
        val copy = Configuration(configuration)
        copy.setLocales(LocaleList(Locale.forLanguageTag(languageTag)))
        baseContext.createConfigurationContext(copy)
    }
    CompositionLocalProvider(LocalContext provides localizedContext) {
        key(languageTag) { content() }
    }
}

@Composable
private fun StatusScreen(status: StatusDisplayModel) {
    PageList {
        item { PageHeader(R.string.status_title, R.string.status_subtitle, Icons.Rounded.Favorite) }
        item { PressureCard(status) }
        item { TrendCard(status.trend) }
    }
}

@Composable
private fun PressureCard(status: StatusDisplayModel) {
    val current = status.current
    val accent = stressColor(current.band)
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.30f), MaterialTheme.colorScheme.surfaceContainerLow),
                    ),
                )
                .padding(24.dp),
        ) {
            Text(stringResource(R.string.status_current_week), style = MaterialTheme.typography.titleMedium)
            if (current.score == null) {
                Text(
                    stringResource(R.string.status_no_score),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.status_score_format, current.score),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.status_out_of_ten),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(color = accent, shape = CircleShape) {
                        Text(
                            stressBandLabel(current.band),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((current.score / 10.0).toFloat().coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.tertiary,
                                        MaterialTheme.colorScheme.error,
                                    ),
                                ),
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendCard(points: List<WeeklyStressPoint>) {
    SectionCard(Icons.Rounded.Timeline, stringResource(R.string.status_recent_trend)) {
        Text(
            stringResource(R.string.status_recent_trend_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val available = points.filter { it.score != null }
        if (available.isEmpty()) {
            Text(
                stringResource(R.string.status_no_trend),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            val primary = MaterialTheme.colorScheme.primary
            val grid = MaterialTheme.colorScheme.outlineVariant
            Canvas(Modifier.fillMaxWidth().height(190.dp)) {
                repeat(3) { row ->
                    val y = size.height * row / 2f
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                val path = Path()
                var started = false
                points.forEachIndexed { index, point ->
                    val score = point.score ?: return@forEachIndexed
                    val x = if (points.size == 1) size.width / 2 else size.width * index / (points.size - 1)
                    val y = size.height * (1f - (score / 10.0).toFloat())
                    if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
                }
                drawPath(path, primary, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                points.forEachIndexed { index, point ->
                    point.score?.let { score ->
                        val x = if (points.size == 1) size.width / 2 else size.width * index / (points.size - 1)
                        val y = size.height * (1f - (score / 10.0).toFloat())
                        drawCircle(primary, 5.dp.toPx(), Offset(x, y))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                points.firstOrNull()?.let { Text(formatWeekLabel(it.weekStart), style = MaterialTheme.typography.labelSmall) }
                points.lastOrNull()?.let { Text(formatWeekLabel(it.weekStart), style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
private fun PracticeScreen(
    uiState: NeuroTrackUiState,
    session: MindfulnessSessionUiState,
    latestSubmission: AssessmentHistoryItem?,
    onSubmit: (List<Int>) -> Unit,
    onDismissSubmission: () -> Unit,
    onStartMindfulness: (Int) -> Unit,
    onClearSessionResult: () -> Unit,
) {
    var showAssessment by rememberSaveable { mutableStateOf(false) }
    if (showAssessment) {
        AssessmentScreen(onBack = { showAssessment = false }, onSubmit = onSubmit)
        return
    }

    latestSubmission?.let {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.assessment_result_title)) },
            text = { Text(stringResource(R.string.assessment_result_body)) },
            confirmButton = {
                Button(onClick = onDismissSubmission) { Text(stringResource(R.string.common_done)) }
            },
        )
    }
    session.lastResult?.let { result ->
        AlertDialog(
            onDismissRequest = onClearSessionResult,
            text = {
                Text(
                    when (result) {
                        MindfulnessSessionStatus.COMPLETED -> stringResource(R.string.session_completed)
                        MindfulnessSessionStatus.INTERRUPTED -> stringResource(R.string.session_interrupted)
                        else -> stringResource(R.string.session_abandoned)
                    },
                )
            },
            confirmButton = {
                Button(onClick = onClearSessionResult) { Text(stringResource(R.string.common_done)) }
            },
        )
    }

    val thisWeekReviewed = uiState.status.current.assessmentScore != null
    val currentWeekStart = MindfulnessSchedule.weekStart(uiState.today)
    val completedPracticeDates = remember(uiState.sessions, currentWeekStart) {
        MindfulnessSchedule.completedPracticeDates(currentWeekStart, uiState.sessions)
    }
    val isPracticeDay = MindfulnessSchedule.isPracticeDay(uiState.today)
    var duration by rememberSaveable { mutableIntStateOf(10) }
    PageList {
        item { PageHeader(R.string.practice_title, R.string.practice_subtitle, Icons.Rounded.SelfImprovement) }
        item { WeeklyRhythm(currentWeekStart, completedPracticeDates) }
        item {
            FeatureCard(
                icon = Icons.Rounded.CheckCircle,
                title = stringResource(R.string.weekly_review_title),
                body = stringResource(R.string.weekly_review_desc),
                accent = MaterialTheme.colorScheme.tertiary,
            ) {
                if (thisWeekReviewed) {
                    Text(
                        stringResource(R.string.weekly_review_done),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Button(onClick = { showAssessment = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (thisWeekReviewed) R.string.weekly_review_again else R.string.weekly_review_start))
                }
            }
        }
        item {
            FeatureCard(
                icon = Icons.Rounded.SelfImprovement,
                title = stringResource(R.string.mindfulness_title),
                body = stringResource(R.string.mindfulness_desc),
                accent = MaterialTheme.colorScheme.primary,
            ) {
                Text(stringResource(R.string.mindfulness_duration), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15).forEach { minutes ->
                        ChoicePill(
                            selected = duration == minutes,
                            text = stringResource(R.string.mindfulness_minutes, minutes),
                            onClick = { duration = minutes },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (!isPracticeDay) {
                    Text(
                        stringResource(R.string.mindfulness_off_day),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = { onStartMindfulness(duration) },
                    enabled = isPracticeDay,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.SelfImprovement, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.mindfulness_start))
                }
            }
        }
    }
}

@Composable
private fun WeeklyRhythm(weekStart: LocalDate, completedDates: Set<LocalDate>) {
    SectionCard(Icons.Rounded.Timeline, stringResource(R.string.practice_weekly_rhythm)) {
        Text(
            stringResource(R.string.practice_completed_format, completedDates.size),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(stringResource(R.string.practice_schedule), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val labels = listOf(
                R.string.practice_day_mon,
                R.string.practice_day_wed,
                R.string.practice_day_fri,
                R.string.practice_day_sun,
            )
            MindfulnessSchedule.practiceDates(weekStart).zip(labels).forEach { (date, label) ->
                val completed = date in completedDates
                Surface(
                    shape = CircleShape,
                    color = if (completed) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (completed) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                        else Text(stringResource(label), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssessmentScreen(onBack: () -> Unit, onSubmit: (List<Int>) -> Unit) {
    val questions = stringArrayResource(R.array.assessment_questions)
    val options = listOf(
        stringArrayResource(R.array.assessment_sleep_options).toList(),
        stringArrayResource(R.array.assessment_phone_options).toList(),
        stringArrayResource(R.array.assessment_views_options).toList(),
        stringArrayResource(R.array.assessment_events_options).toList(),
        stringArrayResource(R.array.assessment_environment_options).toList(),
        stringArrayResource(R.array.assessment_work_options).toList(),
        stringArrayResource(R.array.assessment_relationship_options).toList(),
        stringArrayResource(R.array.assessment_body_options).toList(),
        stringArrayResource(R.array.assessment_recovery_options).toList(),
        stringArrayResource(R.array.assessment_worry_options).toList(),
    )
    var answers by rememberSaveable { mutableStateOf(List(10) { -1 }) }
    var current by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(onBack = onBack)
    PageList {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
                Column {
                    Text(stringResource(R.string.assessment_title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.assessment_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Text(stringResource(R.string.assessment_progress, current + 1, questions.size), style = MaterialTheme.typography.labelLarge)
            Box(Modifier.fillMaxWidth().height(7.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)) {
                Box(
                    Modifier.fillMaxWidth((current + 1f) / questions.size).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }
        item {
            Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(questions[current], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    options[current].forEachIndexed { index, option ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (answers[current] == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth().clickable {
                                answers = replaceAssessmentAnswer(answers, current, index)
                            },
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(option, modifier = Modifier.weight(1f))
                                if (answers[current] == index) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { current-- },
                    enabled = current > 0,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.assessment_previous)) }
                Button(
                    onClick = {
                        if (current == questions.lastIndex) { onSubmit(answers); onBack() } else current++
                    },
                    enabled = canAdvanceAssessment(answers, current) &&
                        (current < questions.lastIndex || canSubmitAssessment(answers)),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(if (current == questions.lastIndex) R.string.assessment_submit else R.string.assessment_next))
                    if (current < questions.lastIndex) Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                }
            }
        }
    }
}

@Composable
private fun MindfulnessSessionScreen(
    state: MindfulnessSessionUiState,
    onEnd: () -> Unit,
    onFocusUnavailable: () -> Unit = onEnd,
) {
    val context = LocalView.current.context
    val view = LocalView.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        runCatching { activity?.startLockTask() }
        onDispose {
            val lockTaskActive = context.getSystemService(ActivityManager::class.java)
                ?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
            if (lockTaskActive) runCatching { activity?.stopLockTask() }
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(Unit) {
        delay(5_000)
        while (true) {
            val pinned = context.getSystemService(ActivityManager::class.java)
                ?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
            if (!pinned) {
                onFocusUnavailable()
                break
            }
            delay(1_000)
        }
    }
    BackHandler(enabled = true) {}
    val transition = rememberInfiniteTransition(label = "breathing")
    val breath by transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_000), RepeatMode.Reverse),
        label = "breath",
    )
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background),
            ),
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(28.dp),
        ) {
            Text(stringResource(R.string.session_focus_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.session_focus_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(44.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(accent.copy(alpha = 0.10f), radius = size.minDimension / 2 * breath)
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * state.progress,
                        useCenter = false,
                        style = Stroke(9.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Text(
                    stringResource(R.string.session_time_format, state.remainingSeconds / 60, state.remainingSeconds % 60),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MusicNote, null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.session_music), color = accent)
            }
            Spacer(Modifier.height(36.dp))
            OutlinedButton(onClick = onEnd) { Text(stringResource(R.string.session_end_early)) }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: com.example.neurotrack.AppSettings,
    onLanguage: (String) -> Unit,
    onTheme: (String) -> Unit,
    onReminder: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    var notificationGranted by remember { mutableStateOf(context.notificationsGranted()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationGranted = it
    }
    PageList {
        item { PageHeader(R.string.settings_title, R.string.settings_subtitle, Icons.Rounded.Settings) }
        item {
            SectionCard(Icons.Rounded.Notifications, stringResource(R.string.settings_reminder)) {
                Text(stringResource(R.string.settings_reminder_days), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.settings_reminder_time), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberDropdown(settings.reminderHour, (0..23).toList(), { "%02d".format(it) }, Modifier.weight(1f)) {
                        onReminder(it, settings.reminderMinute)
                    }
                    Text(":", style = MaterialTheme.typography.headlineSmall)
                    val minutes = listOf(0, 15, 30, 45)
                    NumberDropdown(settings.reminderMinute, minutes, { "%02d".format(it) }, Modifier.weight(1f)) {
                        onReminder(settings.reminderHour, it)
                    }
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_permission), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.settings_permission_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }, enabled = !notificationGranted) {
                        Text(stringResource(if (notificationGranted) R.string.permission_granted else R.string.permission_missing))
                    }
                }
            }
        }
        item {
            SectionCard(Icons.Rounded.Language, stringResource(R.string.settings_language)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoicePill(settings.languageTag == SettingsStore.LANGUAGE_ZH, stringResource(R.string.language_zh), { onLanguage(SettingsStore.LANGUAGE_ZH) }, Modifier.weight(1f))
                    ChoicePill(settings.languageTag == SettingsStore.LANGUAGE_EN, stringResource(R.string.language_en), { onLanguage(SettingsStore.LANGUAGE_EN) }, Modifier.weight(1f))
                }
            }
        }
        item {
            SectionCard(Icons.Rounded.Palette, stringResource(R.string.settings_theme)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        SettingsStore.THEME_SYSTEM to R.string.theme_system,
                        SettingsStore.THEME_LIGHT to R.string.theme_light,
                        SettingsStore.THEME_DARK to R.string.theme_dark,
                    ).forEach { (value, label) ->
                        ChoicePill(settings.themeMode == value, stringResource(label), { onTheme(value) }, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            SectionCard(Icons.Rounded.Favorite, stringResource(R.string.settings_about)) {
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
                Text(stringResource(R.string.disclaimer), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NumberDropdown(
    value: Int,
    options: List<Int>,
    formatter: (Int) -> String,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(formatter(value)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(formatter(option)) }, onClick = { expanded = false; onSelect(option) })
            }
        }
    }
}

@Composable
private fun ChoicePill(selected: Boolean, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.heightIn(min = 46.dp).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    body: String,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.16f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent) }
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun SectionCard(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, modifier = Modifier.size(21.dp)) }
                }
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun PageHeader(title: Int, subtitle: Int, icon: ImageVector) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Text(stringResource(title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PageList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content,
            modifier = Modifier.fillMaxHeight().widthIn(max = 780.dp).align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun stressColor(band: StressBand?): Color = when (band) {
    StressBand.LOW -> MaterialTheme.colorScheme.secondary
    StressBand.MEDIUM -> MaterialTheme.colorScheme.tertiary
    StressBand.HIGH -> MaterialTheme.colorScheme.error
    null -> MaterialTheme.colorScheme.primary
}

@Composable
private fun stressBandLabel(band: StressBand?): String = stringResource(
    when (band) {
        StressBand.LOW -> R.string.status_band_low
        StressBand.MEDIUM -> R.string.status_band_medium
        StressBand.HIGH -> R.string.status_band_high
        null -> R.string.status_no_score
    },
)

private fun Context.notificationsGranted(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
