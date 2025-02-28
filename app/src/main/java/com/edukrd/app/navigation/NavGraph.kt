package com.edukrd.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.edukrd.app.ui.screens.*

sealed class AppScreen(val route: String) {
    object Login : AppScreen("login")
    object Register : AppScreen("register")
    object ForgotPassword : AppScreen("forgot_password")
    object Home : AppScreen("home")

    object Course : AppScreen("course/{courseId}") {
        fun createRoute(courseId: String) = "course/$courseId"
    }

    object Exam : AppScreen("exam/{userId}/{courseId}") {
        fun createRoute(userId: String, courseId: String) = "exam/$userId/$courseId"
    }

    object Error : AppScreen("error_screen")
}

@Composable
fun NavGraph(navController: NavHostController, startDestination: String) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppScreen.Login.route) { LoginScreen(navController) }
        composable(AppScreen.Register.route) { RegisterScreen(navController) }
        composable(AppScreen.ForgotPassword.route) { ForgotPasswordScreen(navController) }
        composable(AppScreen.Home.route) { MainHomeScreen(navController) }

        composable(AppScreen.Course.route) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")
            if (courseId.isNullOrEmpty()) {
                ErrorScreen(navController)
            } else {
                CourseScreen(navController, courseId)
            }
        }

        composable(AppScreen.Exam.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            val courseId = backStackEntry.arguments?.getString("courseId")

            if (userId.isNullOrEmpty() || courseId.isNullOrEmpty()) {
                ErrorScreen(navController)
            } else {
                ExamScreen(navController, userId, courseId)
            }
        }

        composable(AppScreen.Error.route) { ErrorScreen(navController) }
    }
}
