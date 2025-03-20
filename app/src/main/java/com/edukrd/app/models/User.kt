package com.edukrd.app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val name: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val sector: String = "",
    val phone: String = "",
    val email: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val coins: Int = 0,
    val primerAcceso: Boolean = true,  // Default value añadido
    val notificationsEnabled: Boolean = false,
    val notificationFrequency: String = "Diaria",
    val themePreference: String = "light"
) {
    // Constructor sin argumentos requerido por Firestore
    constructor() : this(
        name = "",
        lastName = "",
        birthDate = "",
        sector = "",
        phone = "",
        email = "",
        createdAt = Timestamp.now(),
        coins = 0,
        primerAcceso = true,
        notificationsEnabled = false,
        notificationFrequency = "Diaria",
        themePreference = "light"
    )
}