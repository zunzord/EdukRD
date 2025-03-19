package com.edukrd.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.edukrd.app.ui.screens.*

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    themeViewModel: com.edukrd.app.viewmodel.ThemeViewModel // Recibe el ThemeViewModel global
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController, themeViewModel)
        }
        composable(Screen.Store.route) {
            StoreScreen(navController)
        }
        composable(Screen.Medals.route) {
            MedalsScreen(navController)
        }
        composable(Screen.Ranking.route) {
            RankingScreen(navController)
        }
        composable(Screen.Course.route) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")
            if (courseId.isNullOrEmpty()) {
                ErrorScreen(navController)
            } else {
                CourseScreen(navController, courseId)
            }
        }
        composable(Screen.Exam.route) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")
            if (courseId.isNullOrEmpty()) {
                ErrorScreen(navController)
            } else {
                ExamScreen(navController, courseId)
            }
        }
        composable(Screen.Error.route) {
            ErrorScreen(navController)
        }
    }
}
