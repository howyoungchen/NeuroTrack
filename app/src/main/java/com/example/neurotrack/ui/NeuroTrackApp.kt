package com.example.neurotrack.ui

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
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
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.neurotrack.R
import com.example.neurotrack.domain.MindfulnessSessionStatus
import com.example.neurotrack.domain.MindfulnessSchedule
import com.example.neurotrack.domain.StressBand
import com.example.neurotrack.domain.WeeklyStressPoint
import com.example.neurotrack.mindfulness.MindfulnessLessons
import kotlinx.coroutines.delay
import java.time.DayOfWeek
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
                onTogglePlayback = viewModel::toggleMindfulnessPlayback,
                onRestart = viewModel::restartMindfulness,
            )
            return@LocalizedResources
        }
        val uiState by viewModel.uiState.collectAsState()
        val assessmentSaved by viewModel.assessmentSaved.collectAsState()
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
                    AppScreen.STATUS -> StatusScreen(uiState.status, settings.refreshDay)
                    AppScreen.PRACTICE -> PracticeScreen(
                        uiState = uiState,
                        session = session,
                        refreshDay = settings.refreshDay,
                        assessmentSaved = assessmentSaved,
                        onSubmit = viewModel::submitAssessment,
                        onDismissSubmission = viewModel::clearAssessmentSaved,
                        onStartMindfulness = viewModel::startMindfulness,
                        onClearSessionResult = viewModel::clearSessionResult,
                    )
                    AppScreen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onRefreshDay = viewModel::setRefreshDay,
                        onLanguage = viewModel::setLanguage,
                        onTheme = viewModel::setThemeMode,
                    )
                }
            }
        }
    }
}

@Composable
internal fun LocalizedResources(languageTag: String, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val configuration = LocalConfiguration.current
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current
    val localizedContext = remember(baseContext, configuration, languageTag) {
        val copy = Configuration(configuration)
        copy.setLocales(LocaleList(Locale.forLanguageTag(languageTag)))
        baseContext.createConfigurationContext(copy)
    }
    val localizedContent: @Composable () -> Unit = {
        key(languageTag) { content() }
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
private fun StatusScreen(status: StatusDisplayModel, refreshDay: DayOfWeek) {
    PageList {
        item {
            PageHeader(
                title = R.string.status_title,
                subtitle = stringResource(
                    R.string.status_subtitle,
                    weekdayLabel(refreshDay),
                ),
                icon = Icons.Rounded.Favorite,
            )
        }
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
    refreshDay: DayOfWeek,
    assessmentSaved: Boolean,
    onSubmit: (List<Int>) -> Unit,
    onDismissSubmission: () -> Unit,
    onStartMindfulness: (Int) -> Unit,
    onClearSessionResult: () -> Unit,
) {
    val assessmentDayLabel = weekdayLabel(MindfulnessSchedule.assessmentDay(refreshDay))
    val refreshDayLabel = weekdayLabel(refreshDay)
    var showAssessment by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.assessmentAvailable) {
        if (!uiState.assessmentAvailable) showAssessment = false
    }
    if (showAssessment && uiState.assessmentAvailable) {
        AssessmentScreen(onBack = { showAssessment = false }, onSubmit = onSubmit)
        return
    }

    if (assessmentSaved) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.assessment_result_title)) },
            text = { Text(stringResource(R.string.assessment_result_body, refreshDayLabel)) },
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

    val thisWeekReviewed = uiState.thisWeekReviewed
    val completedLessonIds = uiState.completedLessonIds
    PageList {
        item {
            PageHeader(
                R.string.practice_title,
                stringResource(R.string.practice_subtitle),
                Icons.Rounded.SelfImprovement,
            )
        }
        item {
            MindfulnessRoundCard(
                completedLessonIds = completedLessonIds,
                refreshDayLabel = refreshDayLabel,
                onStartMindfulness = onStartMindfulness,
            )
        }
        item {
            FeatureCard(
                icon = Icons.Rounded.CheckCircle,
                title = stringResource(R.string.weekly_review_title),
                body = stringResource(R.string.weekly_review_desc, assessmentDayLabel),
                accent = MaterialTheme.colorScheme.tertiary,
            ) {
                if (thisWeekReviewed) {
                    Text(
                        stringResource(R.string.weekly_review_done),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (!uiState.assessmentAvailable) {
                    Text(
                        stringResource(R.string.weekly_review_day_only, assessmentDayLabel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = { showAssessment = true },
                    enabled = uiState.assessmentAvailable,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (thisWeekReviewed) R.string.weekly_review_again else R.string.weekly_review_start))
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
    onTogglePlayback: () -> Unit,
    onRestart: () -> Unit,
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
    val lesson = MindfulnessLessons.find(state.lessonId)
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
            Text(
                lesson?.let { stringResource(it.titleRes) } ?: stringResource(R.string.session_focus_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
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
                Icon(Icons.Rounded.Headphones, null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.session_guided_audio), color = accent)
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onTogglePlayback) {
                    Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (state.isPlaying) R.string.session_pause else R.string.session_resume))
                }
                OutlinedButton(onClick = onRestart) {
                    Icon(Icons.Rounded.Replay, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.session_restart))
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onEnd) { Text(stringResource(R.string.session_end_early)) }
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
internal fun PageHeader(title: Int, subtitle: String, icon: ImageVector) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Text(stringResource(title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun weekdayLabel(day: DayOfWeek): String =
    stringArrayResource(R.array.weekday_names)[day.value - 1]

@Composable
internal fun PageList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
