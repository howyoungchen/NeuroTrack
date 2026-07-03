package com.example.neurotrack.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.neurotrack.AppSettings
import com.example.neurotrack.NeuroTrackApplication
import com.example.neurotrack.SettingsStore
import com.example.neurotrack.background.LogExporter
import com.example.neurotrack.background.MonitoringServiceController
import com.example.neurotrack.background.NeuroWorkScheduler
import com.example.neurotrack.background.NotificationHelper
import com.example.neurotrack.data.AssessmentRecordEntity
import com.example.neurotrack.data.NeuroRepository
import com.example.neurotrack.data.SleepRecordEntity
import com.example.neurotrack.domain.StressCalculator
import com.example.neurotrack.domain.StressResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit

data class NeuroTrackUiState(
    val assessments: List<AssessmentRecordEntity> = emptyList(),
    val sleepRecords: List<SleepRecordEntity> = emptyList(),
    val stressResult: StressResult = StressCalculator.calculate(emptyList(), emptyList()),
)

class NeuroTrackViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NeuroTrackApplication
    private val repository: NeuroRepository = app.container.repository
    private val settingsStore: SettingsStore = app.container.settingsStore
    private val _latestSubmission = MutableStateFlow<AssessmentRecordEntity?>(null)
    private val _serviceUptimeMillis = MutableStateFlow(0L)
    private var uptimeJob: Job? = null

    val settings: StateFlow<AppSettings> = settingsStore.settings
    val latestSubmission: StateFlow<AssessmentRecordEntity?> = _latestSubmission

    val uiState: StateFlow<NeuroTrackUiState> = combine(
        repository.assessmentHistory,
        repository.observeSleepRecords(LocalDate.now().minusDays(31).toEpochDay()),
    ) { assessments, sleepRecords ->
        NeuroTrackUiState(
            assessments = assessments,
            sleepRecords = sleepRecords,
            stressResult = StressCalculator.calculate(assessments, sleepRecords),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NeuroTrackUiState(),
    )

    val serviceUptimeMillis: StateFlow<Long> = _serviceUptimeMillis

    init {
        startUptimeTicker()
    }

    fun submitAssessment(answers: List<Int>) {
        viewModelScope.launch {
            val record = repository.submitAssessment(answers)
            _latestSubmission.value = record

            val currentState = uiState.value
            val stress = StressCalculator.calculate(
                assessments = listOf(record) + currentState.assessments.filterNot { it.id == record.id },
                sleepRecords = currentState.sleepRecords,
            )
            val score = stress.score
            if (score != null && score > 5.0) {
                NotificationHelper.showStressWarning(getApplication(), score)
                repository.log("WARN", "StressAlert", "Stress score exceeded threshold: $score")
            }
        }
    }

    fun clearLatestSubmission() {
        _latestSubmission.value = null
    }

    fun setLanguage(languageTag: String) {
        settingsStore.setLanguage(languageTag)
    }

    fun setThemeMode(themeMode: String) {
        settingsStore.setThemeMode(themeMode)
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        MonitoringServiceController.setMonitoringEnabled(getApplication(), settingsStore, enabled)
        viewModelScope.launch {
            repository.log("INFO", "Settings", "Monitoring service enabled=$enabled")
        }
    }

    fun setReminder(dayOfWeek: Int, hour: Int) {
        settingsStore.setReminder(dayOfWeek, hour)
        NeuroWorkScheduler.scheduleAssessmentReminder(getApplication(), settingsStore.settings.value)
        viewModelScope.launch {
            repository.log("INFO", "Settings", "Reminder changed day=$dayOfWeek hour=$hour")
        }
    }

    fun exportLogs(context: Context) {
        viewModelScope.launch {
            val intent = LogExporter.createShareIntent(context, repository)
            context.startActivity(intent)
            repository.log("INFO", "Logs", "Log export shared")
        }
    }

    override fun onCleared() {
        uptimeJob?.cancel()
        super.onCleared()
    }

    private fun startUptimeTicker() {
        uptimeJob = viewModelScope.launch {
            while (true) {
                val startedAt = settingsStore.serviceStartedAtMillis()
                _serviceUptimeMillis.value = if (
                    settingsStore.settings.value.monitoringEnabled &&
                    startedAt > 0L
                ) {
                    System.currentTimeMillis() - startedAt
                } else {
                    0L
                }
                delay(TimeUnit.SECONDS.toMillis(1))
            }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NeuroTrackViewModel::class.java)) {
                        return NeuroTrackViewModel(application) as T
                    }
                    error("Unknown ViewModel class ${modelClass.name}")
                }
            }
    }
}
