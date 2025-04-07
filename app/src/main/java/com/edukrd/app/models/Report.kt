package com.edukrd.app.models

data class Report(
    val reportId: String = "",
    val userId: String = "",
    val email: String = "",
    val phone: String = "",
    val sector: String = "",
    val type: String = "",
    val message: String = "",
    val status: String = "open",
    val createdAt: com.google.firebase.Timestamp? = null
)
