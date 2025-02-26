package com.edukrd.app.models

data class UserProgress(
    val courseId: String = "",
    val userId: String = "",
    val progressPercentage: Int = 0,  // 🟢 Asegúrate de que exista este campo
    val completed: Boolean = false,   // 🟢 Asegúrate de que exista este campo
    val lastAccessed: Long = System.currentTimeMillis() // 🟢 Asegúrate de que exista este campo
)
