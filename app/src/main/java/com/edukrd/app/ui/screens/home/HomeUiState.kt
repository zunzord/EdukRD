package com.edukrd.app.ui.screens.home

import com.edukrd.app.models.Course
import com.edukrd.app.models.User
import com.edukrd.app.models.UserGoalsState

data class HomeUiState(
    val userData: User? = null,
    val userLoading: Boolean = false,
    val courses: List<Course> = emptyList(),
    val passedCourseIds: List<String> = emptyList(),
    val coinRewards: Map<String, Int> = emptyMap(),
    val coursesLoading: Boolean = false,
    val courseError: String? = null,
    val dailyData: List<Float> = emptyList(),
    val weeklyData: List<Float> = emptyList(),
    val monthlyData: List<Float> = emptyList(),
    val userGoalsState: UserGoalsState = UserGoalsState()
)
