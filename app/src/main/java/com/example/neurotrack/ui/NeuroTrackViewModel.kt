package com.example.neurotrack.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.neurotrack.AppSettings
import com.example.neurotrack.NeuroTrackApplication
import com.example.neurotrack.SettingsStore
import com.example.neurotrack.background.MindfulnessScheduler
import com.example.neurotrack.data.AssessmentRecordEntity
import com.example.neurotrack.data.MindfulnessSessionEntity
import com.example.neurotrack.data.NeuroRepository
import com.example.neurotrack.data.toDomain
import com.example.neurotrack.domain.MindfulnessSchedule
import com.example.neurotrack.domain.MindfulnessSessionRecord
import com.example.neurotrack.domain.MindfulnessSessionStatus
import com.example.neurotrack.mindfulness.MindfulnessAudioPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.ceil

data class NeuroTrackUiState(
    val today: LocalDate = LocalDate.now(),
    val assessments: List<AssessmentHistoryItem> = emptyList(),
    val sessions: List<MindfulnessSessionRecord> = emptyList(),
    val status: StatusDisplayModel = StatusDisplayModel.empty(),
)

data class MindfulnessSessionUiState(
    val active: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val lastResult: MindfulnessSessionStatus? = null,
) {
    val progress: Float
        get() = if (totalSeconds == 0) 0f else 1f - remainingSeconds.toFloat() / totalSeconds
}

class NeuroTrackViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NeuroTrackApplication
    private val repository: NeuroRepository = app.container.repository
    private val settingsStore: SettingsStore = app.container.settingsStore
    private val audioPlayer = MindfulnessAudioPlayer()
    private val _latestSubmission = MutableStateFlow<AssessmentHistoryItem?>(null)
    private val _session = MutableStateFlow(MindfulnessSessionUiState())
    private val _clock = MutableStateFlow(LocalDateTime.now())
    private var sessionId: Long? = null
    private var sessionJob: Job? = null
    private var sessionStarting = false

    val settings: StateFlow<AppSettings> = settingsStore.settings
    val latestSubmission: StateFlow<AssessmentHistoryItem?> = _latestSubmission.asStateFlow()
    val session: StateFlow<MindfulnessSessionUiState> = _session.asStateFlow()

    val uiState: StateFlow<NeuroTrackUiState> = combine(
        repository.assessmentHistory,
        repository.mindfulnessHistory,
        settingsStore.settings,
        _clock,
    ) { assessments, sessions, settings, now ->
        NeuroTrackUiState(
            today = now.toLocalDate(),
            assessments = assessments.map(AssessmentRecordEntity::toHistoryItem),
            sessions = sessions.map(MindfulnessSessionEntity::toDomain),
            status = buildStatusDisplayModel(
                assessments = assessments.map { it.toDomain() },
                sessions = sessions.map { it.toDomain() },
                today = now.toLocalDate(),
                dueThroughDate = dueThroughDate(
                    now = now,
                    reminderHour = settings.reminderHour,
                    reminderMinute = settings.reminderMinute,
                ),
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NeuroTrackUiState(),
    )

    fun submitAssessment(answers: List<Int>) {
        viewModelScope.launch {
            val monday = MindfulnessSchedule.weekStart(LocalDate.now())
            val record = repository.submitAssessment(monday.toEpochDay(), answers)
            _latestSubmission.value = record.toHistoryItem()
        }
    }

    fun clearLatestSubmission() {
        _latestSubmission.value = null
    }

    fun startMindfulness(durationMinutes: Int) {
        if (
            _session.value.active ||
            sessionStarting ||
            durationMinutes !in setOf(5, 10, 15) ||
            !MindfulnessSchedule.isPracticeDay(LocalDate.now())
        ) return
        sessionStarting = true
        viewModelScope.launch {
            try {
                val record = repository.startMindfulness(durationMinutes)
                sessionId = record.id
                val totalSeconds = durationMinutes * 60
                _session.value = MindfulnessSessionUiState(
                    active = true,
                    remainingSeconds = totalSeconds,
                    totalSeconds = totalSeconds,
                )
                try {
                    audioPlayer.start()
                } catch (_: RuntimeException) {
                    finishMindfulness(MindfulnessSessionStatus.INTERRUPTED)
                    return@launch
                }
                val finishAt = SystemClock.elapsedRealtime() + totalSeconds * 1_000L
                sessionJob = launch {
                    while (_session.value.active) {
                        val remaining = ceil((finishAt - SystemClock.elapsedRealtime()) / 1_000.0)
                            .toInt()
                            .coerceAtLeast(0)
                        _session.value = _session.value.copy(remainingSeconds = remaining)
                        if (remaining == 0) {
                            viewModelScope.launch {
                                finishMindfulness(MindfulnessSessionStatus.COMPLETED)
                            }
                            break
                        }
                        delay(250)
                    }
                }
            } finally {
                sessionStarting = false
            }
        }
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

    fun setReminderTime(hour: Int, minute: Int) {
        settingsStore.setReminderTime(hour, minute)
        MindfulnessScheduler.schedule(getApplication(), settingsStore.settings.value)
    }

    private suspend fun finishMindfulness(status: MindfulnessSessionStatus) {
        val id = sessionId ?: return
        sessionId = null
        val ticker = sessionJob
        sessionJob = null
        ticker?.cancel()
        audioPlayer.stop()
        repository.finishMindfulness(id, status)
        _session.value = _session.value.copy(
            active = false,
            remainingSeconds = if (status == MindfulnessSessionStatus.COMPLETED) 0 else _session.value.remainingSeconds,
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

private fun AssessmentRecordEntity.toHistoryItem() = AssessmentHistoryItem(
    id = id,
    createdAtMillis = createdAtMillis,
    totalScore = totalScore,
)

internal fun dueThroughDate(
    now: LocalDateTime,
    reminderHour: Int,
    reminderMinute: Int,
): LocalDate {
    val reminderTime = LocalTime.of(reminderHour.coerceIn(0, 23), reminderMinute.coerceIn(0, 59))
    return if (now.toLocalTime().isBefore(reminderTime)) now.toLocalDate().minusDays(1)
    else now.toLocalDate()
}
