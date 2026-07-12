package com.example.neurotrack

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.example.neurotrack.background.NotificationHelper
import com.example.neurotrack.background.NeuroWorkScheduler
import com.example.neurotrack.data.NeuroRepository
import com.example.neurotrack.data.NeuroTrackDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek

class NeuroTrackApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
        NeuroWorkScheduler.schedulePeriodicSleepAnalysis(this)
        NeuroWorkScheduler.scheduleAssessmentReminder(this, container.settingsStore.settings.value)
    }
}

class AppContainer(context: Context) {
    val database: NeuroTrackDatabase = NeuroTrackDatabase.getInstance(context)
    val repository: NeuroRepository = NeuroRepository(database)
    val settingsStore: SettingsStore = SettingsStore(context)
}

data class AppSettings(
    val languageTag: String = SettingsStore.LANGUAGE_ZH,
    val themeMode: String = SettingsStore.THEME_SYSTEM,
    val reminderDayOfWeek: Int = DayOfWeek.SUNDAY.value,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
)

class SettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())

    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setLanguage(languageTag: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageTag).apply()
        _settings.value = load()
    }

    fun setThemeMode(themeMode: String) {
        val normalized = when (themeMode) {
            THEME_LIGHT, THEME_DARK -> themeMode
            else -> THEME_SYSTEM
        }
        prefs.edit().putString(KEY_THEME_MODE, normalized).apply()
        _settings.value = load()
    }

    fun setReminder(dayOfWeek: Int, hour: Int, minute: Int = 0) {
        prefs.edit()
            .putInt(KEY_REMINDER_DAY, dayOfWeek)
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
        _settings.value = load()
    }

    private fun load(): AppSettings =
        AppSettings(
            languageTag = prefs.getString(KEY_LANGUAGE, LANGUAGE_ZH) ?: LANGUAGE_ZH,
            themeMode = prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM,
            reminderDayOfWeek = prefs.getInt(KEY_REMINDER_DAY, DayOfWeek.SUNDAY.value),
            reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 20),
            reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0),
        )

    companion object {
        const val LANGUAGE_ZH = "zh"
        const val LANGUAGE_EN = "en"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        private const val PREFS_NAME = "neurotrack_settings"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_REMINDER_DAY = "reminder_day"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
    }
}
