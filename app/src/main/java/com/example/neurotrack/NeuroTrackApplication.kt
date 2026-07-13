package com.example.neurotrack

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.neurotrack.background.NotificationHelper
import com.example.neurotrack.background.WeeklyRoutineScheduler
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
        val refreshDay = container.settingsStore.settings.value.refreshDay
        NotificationHelper.createChannel(this, refreshDay)
        WeeklyRoutineScheduler.schedule(this, refreshDay)
    }
}

class AppContainer(context: Context) {
    val repository = NeuroRepository(NeuroTrackDatabase.getInstance(context))
    val settingsStore = SettingsStore(context)
}

data class AppSettings(
    val languageTag: String = SettingsStore.LANGUAGE_ZH,
    val themeMode: String = SettingsStore.THEME_SYSTEM,
    val refreshDay: DayOfWeek = DayOfWeek.MONDAY,
)

class SettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext
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

    fun setRefreshDay(value: DayOfWeek) {
        if (_settings.value.refreshDay == value) return
        prefs.edit { putInt(KEY_REFRESH_DAY, value.value) }
        _settings.value = load()
        NotificationHelper.createChannel(appContext, value)
        WeeklyRoutineScheduler.schedule(appContext, value)
    }

    private fun load() = AppSettings(
        languageTag = prefs.getString(KEY_LANGUAGE, LANGUAGE_ZH) ?: LANGUAGE_ZH,
        themeMode = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM,
        refreshDay = DayOfWeek.of(
            prefs.getInt(KEY_REFRESH_DAY, DayOfWeek.MONDAY.value).coerceIn(1, 7),
        ),
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
        private const val KEY_REFRESH_DAY = "refresh_day"
    }
}
