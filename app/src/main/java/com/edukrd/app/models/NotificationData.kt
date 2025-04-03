package com.edukrd.app.models

import java.time.LocalDateTime

/**
 * Modelo que encapsula los datos necesarios para mostrar una notificación,
 * incluyendo la frecuencia de la notificación, título, mensaje y el momento programado.
 */
data class NotificationData(
    val frequency: String, // "Diaria", "Semanal" o "Mensual"
    val title: String,
    val message: String,
    val scheduledTime: LocalDateTime
)