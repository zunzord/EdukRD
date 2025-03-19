package com.edukrd.app.models

import com.google.firebase.Timestamp

/**
 * Refleja los campos que manejas en la colección "users".
 * Ajusta mayúsculas/minúsculas para coincidir con Firestore.
 */
data class User(
    val name: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val sector: String = "",
    val phone: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null,
    val coins: Int = 0,

    // Campos nuevos para notificaciones
    val notificationsEnabled: Boolean = false,
    val notificationFrequency: String = "Diaria", // "Diaria", "Semanal", "Mensual"

    // Preferencia de tema (light u dark)
    val themePreference: String = "light"
)
