package com.example.neurotrack.background

import android.annotation.SuppressLint
import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.neurotrack.AppSettings
import com.example.neurotrack.NeuroTrackApplication
import com.example.neurotrack.R
import com.example.neurotrack.SettingsStore
import com.example.neurotrack.data.NeuroRepository
import com.example.neurotrack.data.NeuroTrackDatabase
import com.example.neurotrack.domain.SleepAnalyzer
import java.io.File
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

const val EXTRA_DESTINATION = "destination"
const val DESTINATION_ASSESSMENT = "assessment"

object NotificationHelper {
    const val WARNING_CHANNEL_ID = "stress_alerts"
    const val REMINDER_CHANNEL_ID = "assessment_reminders"
    private const val OLD_MONITORING_CHANNEL_ID = "screen_monitoring"
    private const val MONITORING_NOTIFICATION_ID = 1001
    private const val REMINDER_NOTIFICATION_ID = 1002
    private const val WARNING_NOTIFICATION_ID = 1003

    fun createChannels(context: Context) {
        NotificationManagerCompat.from(context).cancel(MONITORING_NOTIFICATION_ID)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(OLD_MONITORING_CHANNEL_ID)
        val warning = NotificationChannel(
            WARNING_CHANNEL_ID,
            context.getString(R.string.notification_channel_warning),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_warning_desc)
        }
        val reminder = NotificationChannel(
            REMINDER_CHANNEL_ID,
            context.getString(R.string.notification_channel_reminder),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_reminder_desc)
        }
        manager.createNotificationChannels(listOf(warning, reminder))
    }

    @SuppressLint("MissingPermission")
    fun showAssessmentReminder(context: Context) {
        if (!canPostNotifications(context)) {
            return
        }

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_text))
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent(context, DESTINATION_ASSESSMENT))
            .build()
        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    fun showStressWarning(context: Context, score: Double) {
        if (!canPostNotifications(context)) {
            return
        }

        val notification = NotificationCompat.Builder(context, WARNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.stress_warning_notification_title))
            .setContentText(context.getString(R.string.stress_warning_notification_text, score))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent(context, null))
            .build()
        NotificationManagerCompat.from(context).notify(WARNING_NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun mainPendingIntent(context: Context, destination: String?): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        intent.setPackage(context.packageName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (destination != null) {
            intent.putExtra(EXTRA_DESTINATION, destination)
        }
        return PendingIntent.getActivity(
            context,
            destination?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val settings = SettingsStore(context)
        NeuroWorkScheduler.scheduleDailySleepAnalysis(context)
        NeuroWorkScheduler.scheduleAssessmentReminder(context, settings.settings.value)
    }
}

object NeuroWorkScheduler {
    private const val DAILY_SLEEP_WORK = "daily_sleep_analysis"
    private const val ASSESSMENT_REMINDER_WORK = "weekly_assessment_reminder"

    fun scheduleDailySleepAnalysis(context: Context) {
        val request = PeriodicWorkRequestBuilder<SleepAnalysisWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNextDailyHour(12), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_SLEEP_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun scheduleAssessmentReminder(context: Context, settings: AppSettings) {
        val request = PeriodicWorkRequestBuilder<AssessmentReminderWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(
                delayUntilNextWeeklyTime(
                    settings.reminderDayOfWeek,
                    settings.reminderHour,
                    settings.reminderMinute,
                ),
                TimeUnit.MILLISECONDS,
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ASSESSMENT_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun delayUntilNextDailyHour(hour: Int): Long {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(0)
    }

    private fun delayUntilNextWeeklyTime(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        val day = DayOfWeek.of(dayOfWeek.coerceIn(1, 7))
        var next = now
            .with(TemporalAdjusters.nextOrSame(day))
            .withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusWeeks(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(0)
    }
}

class SleepAnalysisWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = repository(applicationContext)
        return runCatching {
            val targetDate = SleepAnalyzer.targetDateForAnalysis()
            val window = SleepAnalyzer.windowFor(targetDate)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                repository.log("WARN", "SleepWorker", "Screen usage events require Android 9 or newer")
            } else if (!UsageScreenEventReader.hasUsageStatsAccess(applicationContext)) {
                repository.log("WARN", "SleepWorker", "Usage stats access is not granted")
            }
            val usageObservations = UsageScreenEventReader.readSleepObservations(
                context = applicationContext,
                startMillis = window.startMillis,
                endMillis = window.endMillis,
            )
            val locationSignals = LocationSleepSignalReader.readSignals(
                context = applicationContext,
                startMillis = window.startMillis,
                endMillis = window.endMillis,
            )
            val record = SleepAnalyzer.analyze(
                targetDate = targetDate,
                observations = usageObservations.copy(locationSignals = locationSignals),
            )
            repository.saveSleepRecord(record)
            repository.pruneLogs(TimeUnit.DAYS.toMillis(30))
            Result.success()
        }.getOrElse { throwable ->
            repository.log("ERROR", "SleepWorker", "Sleep analysis failed", throwable)
            Result.retry()
        }
    }
}

class AssessmentReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = repository(applicationContext)
        return runCatching {
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            val recentAssessments = repository.getRecentAssessments(cutoff)
            if (recentAssessments.isEmpty()) {
                NotificationHelper.showAssessmentReminder(applicationContext)
                repository.log("INFO", "Reminder", "Assessment reminder notification sent")
            } else {
                repository.log("INFO", "Reminder", "Assessment reminder skipped because a recent assessment exists")
            }
            Result.success()
        }.getOrElse { throwable ->
            repository.log("ERROR", "ReminderWorker", "Assessment reminder failed", throwable)
            Result.retry()
        }
    }
}

object LogExporter {
    suspend fun createShareIntent(context: Context, repository: NeuroRepository): Intent {
        val logs = repository.getRecentLogs(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30))
            .asReversed()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val body = buildString {
            appendLine("NeuroTrack debug log")
            appendLine("Generated: ${LocalDateTime.now().format(formatter)}")
            appendLine()
            logs.forEach { log ->
                val time = Instant.ofEpochMilli(log.timestampMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(formatter)
                appendLine("$time [${log.level}] ${log.tag}: ${log.message}")
                log.stackTrace?.let {
                    appendLine(it)
                }
            }
        }
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "neurotrack-log.txt")
        file.writeText(body)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, context.getString(R.string.share_logs_title))
    }
}

object PermissionIntents {
    fun appSettings(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))

    fun notificationSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
        }

    fun batteryOptimizationSettings(context: Context): Intent {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        }
    }

    fun exactAlarmSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
        }

    fun usageAccessSettings(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun hasUsageStatsAccess(context: Context): Boolean =
        UsageScreenEventReader.hasUsageStatsAccess(context)

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }
}

private fun repository(context: Context): NeuroRepository {
    val app = context.applicationContext as? NeuroTrackApplication
    return app?.container?.repository
        ?: NeuroRepository(NeuroTrackDatabase.getInstance(context))
}
