package com.example.healthieru.work

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
import androidx.work.WorkerParameters
import com.example.healthieru.data.remote.RetrofitClient
import com.example.healthieru.ui.LoginActivity

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // CHANGED: Use getExercises() instead of getAdvice()
        val tip = try {
            val response = RetrofitClient.adviceApi.getExercises()
            response.results.firstOrNull()?.name
        } catch (e: Exception) {
            null
        }
        showNotification(tip ?: "Take a minute to record your weight and heart rate today.")
        return Result.success()
    }

    private fun showNotification(message: String) {
        val ctx = applicationContext

        // On Android 13+ we must have the notification permission
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Health reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val pending = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, LoginActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("HealthierU")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }

    companion object {
        private const val CHANNEL_ID = "health_reminders"
    }
}