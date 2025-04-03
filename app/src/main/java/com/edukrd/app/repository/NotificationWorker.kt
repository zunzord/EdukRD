package com.edukrd.app.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.edukrd.app.R

/**
 * NotificationWorker se encarga de mostrar la notificación cuando se activa el WorkManager.
 * Lee los datos de entrada (título y mensaje) y construye la notificación.
 */
class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.d("NotificationWorker", "doWork() started")

        // Obtiene los datos de entrada enviados desde el WorkRequest.
        val title = inputData.getString("title") ?: "Notificación"
        val message = inputData.getString("message") ?: ""
        Log.d("NotificationWorker", "InputData received: title='$title', message='$message'")

        // Crea el canal de notificaciones (para API 26+).
        createNotificationChannel()
        Log.d("NotificationWorker", "Notification channel created")

        // Construye la notificación.
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        Log.d("NotificationWorker", "Notification built")

        // Muestra la notificación.
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d("NotificationWorker", "Notification displayed with ID: $NOTIFICATION_ID")

        return Result.success()
    }

    /**
     * Crea el canal de notificaciones, necesario para dispositivos con API 26 o superior.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Edukrd Notifications"
            val descriptionText = "Canal de notificaciones de Edukrd"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "EDUKRD_CHANNEL"
        private const val NOTIFICATION_ID = 1
    }
}
