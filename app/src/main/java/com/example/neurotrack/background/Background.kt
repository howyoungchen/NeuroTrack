package com.example.neurotrack.background

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.neurotrack.R
import com.example.neurotrack.SettingsStore
import com.example.neurotrack.data.NeuroRepository
import com.example.neurotrack.data.NeuroTrackDatabase
import com.example.neurotrack.domain.MindfulnessSchedule
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

const val EXTRA_DESTINATION = "destination"
const val DESTINATION_PRACTICE = "practice"

object NotificationHelper {
    const val CHANNEL_ID = "mindfulness_reminders"
    private const val NOTIFICATION_ID = 2100

    fun createChannel(context: Context, refreshDay: DayOfWeek) {
        val assessmentDay = MindfulnessSchedule.assessmentDay(refreshDay)
        val assessmentDayLabel = context.resources
            .getStringArray(R.array.weekday_names)[assessmentDay.value - 1]
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_weekly_review),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(
                    R.string.notification_channel_weekly_review_desc,
                    assessmentDayLabel,
                )
            },
        )
    }

    @SuppressLint("MissingPermission")
    fun showWeeklyReviewReminder(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.putExtra(EXTRA_DESTINATION, DESTINATION_PRACTICE)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.weekly_review_reminder_title))
            .setContentText(context.getString(R.string.weekly_review_reminder_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

object WeeklyRoutineScheduler {
    private const val WORK_NAME = "weekly_review_reminder"

    fun schedule(context: Context, refreshDay: DayOfWeek) {
        val workManager = WorkManager.getInstance(context)
        val request = PeriodicWorkRequestBuilder<WeeklyReviewReminderWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(
                delayUntilNextReminder(LocalDateTime.now(), refreshDay),
                TimeUnit.MILLISECONDS,
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    internal fun delayUntilNextReminder(
        now: LocalDateTime,
        refreshDay: DayOfWeek,
    ): Long = Duration.between(
        now,
        MindfulnessSchedule.nextAssessmentReminder(now, refreshDay),
    )
            .toMillis()
            .coerceAtLeast(0)
}

class WeeklyReviewReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val now = LocalDateTime.now()
        val refreshDay = SettingsStore(applicationContext).settings.value.refreshDay
        if (!MindfulnessSchedule.isAssessmentReminderWindow(now, refreshDay)) return Result.success()
        val repository = NeuroRepository(NeuroTrackDatabase.getInstance(applicationContext))
        val weekStart = MindfulnessSchedule.weekStart(now, refreshDay)
        if (!repository.hasAssessment(weekStart.toEpochDay())) {
            NotificationHelper.showWeeklyReviewReminder(applicationContext)
        }
        return Result.success()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) {
            val refreshDay = SettingsStore(context).settings.value.refreshDay
            WeeklyRoutineScheduler.schedule(context, refreshDay)
        }
    }
}
