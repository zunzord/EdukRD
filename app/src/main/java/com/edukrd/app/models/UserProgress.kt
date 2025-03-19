package com.edukrd.app.models

data class UserProgress(
    val courseId: String = "",
    val userId: String = "",
    val progressPercentage: Int = 0,
    val completed: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis()
)
