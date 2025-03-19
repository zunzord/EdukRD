package com.edukrd.app.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object Settings : Screen("settings")
    object Store : Screen("store")
    object Medals : Screen("medals")
    object Ranking : Screen("ranking")
    object Course : Screen("course/{courseId}") {
        fun createRoute(courseId: String) = "course/$courseId"
    }
    object Exam : Screen("exam/{courseId}") {
        fun createRoute(courseId: String) = "exam/$courseId"
    }
    object Error : Screen("error_screen")

    object VerificationPending : Screen("verification_pending?email={email}") {
        fun createRoute(email: String) = "verification_pending?email=$email"
    }



}
