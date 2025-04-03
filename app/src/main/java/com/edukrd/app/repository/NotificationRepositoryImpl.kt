package com.edukrd.app.repository

import android.content.Context
import android.util.Log
import androidx.work.*
import com.edukrd.app.models.NotificationData
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val context: Context
) : NotificationRepository {

    override suspend fun scheduleNotification(notificationData: NotificationData) {
        // Calcula el delay en milisegundos hasta la hora programada.
        val now = LocalDateTime.now()
        val delayDuration = Duration.between(now, notificationData.scheduledTime).toMillis()
        val delayMillis = if (delayDuration > 0) delayDuration else 0L

        Log.d("NotificationRepository", "Scheduling notification:")
        Log.d("NotificationRepository", "Now: $now")
        Log.d("NotificationRepository", "Scheduled Time: ${notificationData.scheduledTime}")
        Log.d("NotificationRepository", "Delay (ms): $delayMillis")
        Log.d("NotificationRepository", "Title: ${notificationData.title}")
        Log.d("NotificationRepository", "Message: ${notificationData.message}")

        // Prepara los datos para el Worker.
        val inputData = workDataOf(
            "title" to notificationData.title,
            "message" to notificationData.message
        )
        Log.d("NotificationRepository", "InputData prepared: $inputData")

        // Construye el WorkRequest para notificaciones.
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        // Encola la tarea de notificación, reemplazando la existente.
        WorkManager.getInstance(context).enqueueUniqueWork(
            "NotificationWork",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        Log.d("NotificationRepository", "Notification Work enqueued with unique name 'NotificationWork'")
    }

    override suspend fun cancelNotification() {
        WorkManager.getInstance(context).cancelUniqueWork("NotificationWork")
        Log.d("NotificationRepository", "Notification Work canceled for unique name 'NotificationWork'")
    }
}
