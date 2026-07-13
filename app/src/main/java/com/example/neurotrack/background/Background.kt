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
import com.example.neurotrack.AppSettings
import com.example.neurotrack.R
import com.example.neurotrack.SettingsStore
import com.example.neurotrack.domain.MindfulnessSchedule
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

const val EXTRA_DESTINATION = "destination"
const val DESTINATION_PRACTICE = "practice"

object NotificationHelper {
    const val CHANNEL_ID = "mindfulness_reminders"
    private const val NOTIFICATION_ID = 2100

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_mindfulness),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_mindfulness_desc)
            },
        )
    }

    @SuppressLint("MissingPermission")
    fun showMindfulnessReminder(context: Context) {
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
            .setContentTitle(context.getString(R.string.mindfulness_reminder_title))
            .setContentText(context.getString(R.string.mindfulness_reminder_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

object MindfulnessScheduler {
    private const val WORK_PREFIX = "mindfulness_reminder_"

    fun schedule(context: Context, settings: AppSettings) {
        MindfulnessSchedule.practiceDays.forEach { day ->
            val request = PeriodicWorkRequestBuilder<MindfulnessReminderWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(
                    delayUntil(day, settings.reminderHour, settings.reminderMinute),
                    TimeUnit.MILLISECONDS,
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_PREFIX + day.name,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }

    private fun delayUntil(day: DayOfWeek, hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now
            .with(TemporalAdjusters.nextOrSame(day))
            .withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) target = target.plusWeeks(1)
        return Duration.between(now, target).toMillis().coerceAtLeast(0)
    }
}

class MindfulnessReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        NotificationHelper.showMindfulnessReminder(applicationContext)
        return Result.success()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) {
            MindfulnessScheduler.schedule(context, SettingsStore(context).settings.value)
        }
    }
}
