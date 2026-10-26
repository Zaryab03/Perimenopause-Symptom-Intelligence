package com.perimenopauseintelligence.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.perimenopauseintelligence.app.App
import com.perimenopauseintelligence.app.MainActivity
import com.perimenopauseintelligence.app.R
import com.perimenopauseintelligence.app.data.SettingsStore
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context, private val settings: SettingsStore) {

    fun reschedule() {
        val wm = WorkManager.getInstance(context)
        if (!settings.reminderEnabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(settings.reminderHour, settings.reminderMinute))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()
        wm.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build(),
        )
    }

    companion object {
        const val WORK_NAME = "daily_checkin_reminder"
        const val CHANNEL_ID = "tracking_reminders"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Tracking reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Optional, gentle check-in reminders at a time you choose."
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as App).container
        if (!container.settings.reminderEnabled) return Result.success()
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val open = PendingIntent.getActivity(
                applicationContext, 0, Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            // Deliberately generic: no symptom details, no guilt, no streaks.
            val publicVersion = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Check-in")
                .build()
            val n = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Check-in")
                .setContentText("A quick check-in, whenever you're ready.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
                .build()
            applicationContext.getSystemService(NotificationManager::class.java).notify(1, n)
        }
        container.reminders.reschedule()
        return Result.success()
    }
}
