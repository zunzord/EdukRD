package com.edukrd.app.models

data class UserGoalsState(
    val dailyTarget: Int = 1,
    val dailyCurrent: Int = 0,

    val weeklyTarget: Int = 1,
    val weeklyCurrent: Int = 0,

    val monthlyTarget: Int = 1,
    val monthlyCurrent: Int = 0,
    val globalProgress: Float = 0f
)
