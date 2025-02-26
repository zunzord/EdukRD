package com.edukrd.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.edukrd.app.ui.screens.LoginScreen
import com.edukrd.app.ui.screens.RegisterScreen
import com.edukrd.app.ui.screens.ForgotPasswordScreen
import com.edukrd.app.ui.screens.MainHomeScreen
import com.edukrd.app.ui.screens.ErrorScreen
import com.edukrd.app.ui.screens.CourseScreen

sealed class AppScreen(val route: String) {
    object Login : AppScreen("login")
    object Register : AppScreen("register")
    object ForgotPassword : AppScreen("forgot_password")
    object Home : AppScreen("home")
    object Course : AppScreen("course/{courseId}") {
        fun createRoute(courseId: String) = "course/$courseId"
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
        composable(AppScreen.Home.route) { MainHomeScreen(navController) } // Reemplazamos HomeScreen
        composable(AppScreen.Course.route) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")

            if (courseId.isNullOrEmpty()) {
                ErrorScreen(navController) // Muestra la pantalla de error
            } else {
                CourseScreen(navController, courseId)
            }
        }

        composable(AppScreen.Error.route) { ErrorScreen(navController) }
    }
}
