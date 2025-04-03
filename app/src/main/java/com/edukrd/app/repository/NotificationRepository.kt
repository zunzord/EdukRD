package com.edukrd.app.repository

import com.edukrd.app.models.NotificationData

interface NotificationRepository {
    suspend fun scheduleNotification(notificationData: NotificationData)
    suspend fun cancelNotification()
}
