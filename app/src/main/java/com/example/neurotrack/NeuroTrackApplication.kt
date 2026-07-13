package com.example.neurotrack

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.neurotrack.background.MindfulnessScheduler
import com.example.neurotrack.background.NotificationHelper
import com.example.neurotrack.data.NeuroRepository
import com.example.neurotrack.data.NeuroTrackDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NeuroTrackApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
        MindfulnessScheduler.schedule(this, container.settingsStore.settings.value)
    }
}

class AppContainer(context: Context) {
    val repository = NeuroRepository(NeuroTrackDatabase.getInstance(context))
    val settingsStore = SettingsStore(context)
}

data class AppSettings(
    val languageTag: String = SettingsStore.LANGUAGE_ZH,
    val themeMode: String = SettingsStore.THEME_SYSTEM,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
)

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setLanguage(value: String) {
        prefs.edit { putString(KEY_LANGUAGE, value) }
        _settings.value = load()
    }

    fun setThemeMode(value: String) {
        val safeValue = value.takeIf { it in setOf(THEME_SYSTEM, THEME_LIGHT, THEME_DARK) } ?: THEME_SYSTEM
        prefs.edit { putString(KEY_THEME, safeValue) }
        _settings.value = load()
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit {
            putInt(KEY_REMINDER_HOUR, hour.coerceIn(0, 23))
            putInt(KEY_REMINDER_MINUTE, minute.coerceIn(0, 59))
        }
        _settings.value = load()
    }

    private fun load() = AppSettings(
        languageTag = prefs.getString(KEY_LANGUAGE, LANGUAGE_ZH) ?: LANGUAGE_ZH,
        themeMode = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM,
        reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 20).coerceIn(0, 23),
        reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0).coerceIn(0, 59),
    )

    companion object {
        const val LANGUAGE_ZH = "zh"
        const val LANGUAGE_EN = "en"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        private const val PREFS_NAME = "neurotrack_settings_v2"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_REMINDER_HOUR = "mindfulness_hour"
        private const val KEY_REMINDER_MINUTE = "mindfulness_minute"
    }
}
