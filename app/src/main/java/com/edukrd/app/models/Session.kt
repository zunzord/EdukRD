package com.edukrd.app.models

data class Session(
    val sessionId: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val active: Boolean = true,
    val createdAt: com.google.firebase.Timestamp? = null,
    val lastUpdate: com.google.firebase.Timestamp? = null
)