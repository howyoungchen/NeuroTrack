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
import com.example.neurotrack.background.NeuroWorkScheduler
import com.example.neurotrack.background.NotificationHelper
import com.example.neurotrack.background.SleepRawDataExporter
import com.example.neurotrack.data.AssessmentRecordEntity
import com.example.neurotrack.data.NeuroRepository
import com.example.neurotrack.data.toDomainAssessment
import com.example.neurotrack.data.toDomainSleepRecord
import com.example.neurotrack.domain.SleepRecord
import com.example.neurotrack.domain.StressCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class NeuroTrackUiState(
    val assessments: List<AssessmentHistoryItem> = emptyList(),
    val sleepRecords: List<SleepRecord> = emptyList(),
    val status: StatusDisplayModel = StatusDisplayModel.empty(),
)

class NeuroTrackViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NeuroTrackApplication
    private val repository: NeuroRepository = app.container.repository
    private val settingsStore: SettingsStore = app.container.settingsStore
    private val _latestSubmission = MutableStateFlow<AssessmentSubmissionDisplay?>(null)

    val settings: StateFlow<AppSettings> = settingsStore.settings
    val latestSubmission: StateFlow<AssessmentSubmissionDisplay?> = _latestSubmission

    val uiState: StateFlow<NeuroTrackUiState> = combine(
        repository.assessmentHistory,
        repository.observeSleepRecords(LocalDate.now().minusDays(31).toEpochDay()),
    ) { assessments, sleepRecords ->
        val domainAssessments = assessments.map { it.toDomainAssessment() }
        val domainSleepRecords = sleepRecords.map { it.toDomainSleepRecord() }
        NeuroTrackUiState(
            assessments = assessments.map { it.toHistoryItem() },
            sleepRecords = domainSleepRecords,
            status = buildStatusDisplayModel(
                assessments = domainAssessments,
                sleepRecords = domainSleepRecords,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NeuroTrackUiState(),
    )

    fun submitAssessment(answers: List<Int>) {
        viewModelScope.launch {
            val record = repository.submitAssessment(answers)
            _latestSubmission.value = AssessmentSubmissionDisplay(
                id = record.id,
                totalScore = record.totalScore,
            )

            val currentState = uiState.value
            val stress = StressCalculator.calculate(
                assessments = listOf(record.toDomainAssessment()) +
                    currentState.assessments
                        .filterNot { it.id == record.id }
                        .map { it.toDomainAssessment() },
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

    fun exportSleepRawData(context: Context, startMillis: Long, endMillis: Long) {
        viewModelScope.launch {
            val intent = SleepRawDataExporter.createShareIntent(context, startMillis, endMillis)
            context.startActivity(intent)
            repository.log("INFO", "SleepRawExport", "Sleep raw data export shared start=$startMillis end=$endMillis")
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

private fun AssessmentRecordEntity.toHistoryItem(): AssessmentHistoryItem =
    AssessmentHistoryItem(
        id = id,
        createdAtMillis = createdAtMillis,
        answersCsv = answersCsv,
        totalScore = totalScore,
    )

private fun AssessmentHistoryItem.toDomainAssessment() =
    com.example.neurotrack.domain.AssessmentScoreRecord(
        createdAtMillis = createdAtMillis,
        totalScore = totalScore,
    )
