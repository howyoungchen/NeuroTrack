package com.example.neurotrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.neurotrack.AppSettings
import com.example.neurotrack.NeuroTrackApplication
import com.example.neurotrack.SettingsStore
import com.example.neurotrack.data.NeuroRepository
import com.example.neurotrack.data.toDomain
import com.example.neurotrack.domain.MindfulnessSchedule
import com.example.neurotrack.domain.MindfulnessSessionStatus
import com.example.neurotrack.mindfulness.MindfulnessAudioPlayer
import com.example.neurotrack.mindfulness.MindfulnessLessons
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime

data class NeuroTrackUiState(
    val assessmentAvailable: Boolean = MindfulnessSchedule.isAssessmentDay(LocalDateTime.now().toLocalDate()),
    val thisWeekReviewed: Boolean = false,
    val completedLessonIds: Set<Int> = emptySet(),
    val status: StatusDisplayModel = StatusDisplayModel.empty(),
)

data class MindfulnessSessionUiState(
    val active: Boolean = false,
    val lessonId: Int = 0,
    val elapsedMillis: Int = 0,
    val totalMillis: Int = 0,
    val isPlaying: Boolean = false,
    val lastResult: MindfulnessSessionStatus? = null,
) {
    val progress: Float
        get() = if (totalMillis <= 0) 0f else {
            (elapsedMillis.toFloat() / totalMillis).coerceIn(0f, 1f)
        }

    val remainingSeconds: Int
        get() = ((totalMillis - elapsedMillis).coerceAtLeast(0) + 999) / 1_000
}

class NeuroTrackViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NeuroTrackApplication
    private val repository: NeuroRepository = app.container.repository
    private val settingsStore: SettingsStore = app.container.settingsStore
    private val audioPlayer = MindfulnessAudioPlayer(application)
    private val _assessmentSaved = MutableStateFlow(false)
    private val _session = MutableStateFlow(MindfulnessSessionUiState())
    private val _clock = MutableStateFlow(LocalDateTime.now())
    private var sessionId: Long? = null
    private var sessionJob: Job? = null
    private var sessionStarting = false

    val settings: StateFlow<AppSettings> = settingsStore.settings
    val assessmentSaved: StateFlow<Boolean> = _assessmentSaved.asStateFlow()
    val session: StateFlow<MindfulnessSessionUiState> = _session.asStateFlow()

    val uiState: StateFlow<NeuroTrackUiState> = combine(
        repository.assessmentHistory,
        repository.mindfulnessHistory,
        _clock,
        settingsStore.settings,
    ) { assessments, sessions, now, settings ->
        val assessmentRecords = assessments.map { it.toDomain() }
        val sessionRecords = sessions.map { it.toDomain() }
        val activeWeekStart = MindfulnessSchedule.weekStart(now, settings.refreshDay)
        NeuroTrackUiState(
            assessmentAvailable = MindfulnessSchedule.isAssessmentDay(
                date = now.toLocalDate(),
                refreshDay = settings.refreshDay,
            ),
            thisWeekReviewed = assessmentRecords.any { it.weekStart == activeWeekStart },
            completedLessonIds = MindfulnessSchedule.completedLessonIds(
                weekStart = activeWeekStart,
                sessions = sessionRecords,
                refreshDay = settings.refreshDay,
            ),
            status = buildStatusDisplayModel(
                assessments = assessmentRecords,
                sessions = sessionRecords,
                now = now,
                refreshDay = settings.refreshDay,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NeuroTrackUiState(),
    )

    fun submitAssessment(answers: List<Int>) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val refreshDay = settingsStore.settings.value.refreshDay
            if (!MindfulnessSchedule.isAssessmentDay(now.toLocalDate(), refreshDay)) return@launch
            val weekStart = MindfulnessSchedule.weekStart(now, refreshDay)
            repository.submitAssessment(weekStart.toEpochDay(), answers)
            _assessmentSaved.value = true
        }
    }

    fun clearAssessmentSaved() {
        _assessmentSaved.value = false
    }

    fun startMindfulness(lessonId: Int) {
        val lesson = MindfulnessLessons.find(lessonId) ?: return
        if (_session.value.active || sessionStarting) return
        sessionStarting = true
        viewModelScope.launch {
            try {
                sessionId = repository.startMindfulness(
                    lessonId = lesson.id,
                    plannedDurationMinutes = lesson.estimatedDurationMinutes,
                )
                _session.value = MindfulnessSessionUiState(
                    active = true,
                    lessonId = lesson.id,
                    isPlaying = true,
                )
                val duration = try {
                    audioPlayer.start(
                        audioResId = lesson.audioRes,
                        languageTag = settingsStore.settings.value.languageTag,
                        onCompletion = {
                            viewModelScope.launch {
                                finishMindfulness(MindfulnessSessionStatus.COMPLETED)
                            }
                        },
                        onError = {
                            viewModelScope.launch {
                                finishMindfulness(MindfulnessSessionStatus.INTERRUPTED)
                            }
                        },
                    )
                } catch (_: RuntimeException) {
                    finishMindfulness(MindfulnessSessionStatus.INTERRUPTED)
                    return@launch
                }
                _session.value = _session.value.copy(totalMillis = duration)
                sessionJob = launch {
                    while (_session.value.active) {
                        _session.value = _session.value.copy(
                            elapsedMillis = audioPlayer.currentPositionMillis,
                            totalMillis = audioPlayer.durationMillis.coerceAtLeast(duration),
                            isPlaying = audioPlayer.isPlaying,
                        )
                        delay(250)
                    }
                }
            } finally {
                sessionStarting = false
            }
        }
    }

    fun toggleMindfulnessPlayback() {
        if (!_session.value.active) return
        _session.value = _session.value.copy(isPlaying = audioPlayer.togglePlayback())
    }

    fun restartMindfulness() {
        if (!_session.value.active) return
        audioPlayer.restart()
        _session.value = _session.value.copy(elapsedMillis = 0, isPlaying = true)
    }

    fun abandonMindfulness() {
        if (_session.value.active) {
            viewModelScope.launch { finishMindfulness(MindfulnessSessionStatus.ABANDONED) }
        }
    }

    fun interruptMindfulness() {
        if (_session.value.active) {
            viewModelScope.launch { finishMindfulness(MindfulnessSessionStatus.INTERRUPTED) }
        }
    }

    fun clearSessionResult() {
        if (!_session.value.active) _session.value = MindfulnessSessionUiState()
    }

    fun setLanguage(value: String) = settingsStore.setLanguage(value)

    fun setThemeMode(value: String) = settingsStore.setThemeMode(value)

    fun setRefreshDay(value: DayOfWeek) = settingsStore.setRefreshDay(value)

    private suspend fun finishMindfulness(status: MindfulnessSessionStatus) {
        val id = sessionId ?: return
        sessionId = null
        val ticker = sessionJob
        sessionJob = null
        ticker?.cancel()
        val finalElapsed = audioPlayer.currentPositionMillis
        val finalTotal = audioPlayer.durationMillis.coerceAtLeast(_session.value.totalMillis)
        audioPlayer.stop()
        repository.finishMindfulness(id, status)
        _session.value = _session.value.copy(
            active = false,
            elapsedMillis = if (status == MindfulnessSessionStatus.COMPLETED) finalTotal else finalElapsed,
            totalMillis = finalTotal,
            isPlaying = false,
            lastResult = status,
        )
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    check(modelClass.isAssignableFrom(NeuroTrackViewModel::class.java))
                    return NeuroTrackViewModel(application) as T
                }
            }
    }

    init {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _clock.value = LocalDateTime.now()
            }
        }
    }
}
