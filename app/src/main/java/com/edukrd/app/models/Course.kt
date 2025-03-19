package com.edukrd.app.models

data class Course(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val totalPages: Int = 0,
    val isPublished: Boolean = false,
    val content: List<Map<String, Any>> = emptyList(),
    val medalla: String = "",
    val recompenza: Int = 0,
    val recompenzaExtra: Int = 0
)
