package com.edukrd.app.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.edukrd.app.ui.screens.CourseScreen
import com.edukrd.app.ui.screens.ErrorScreen
import com.edukrd.app.ui.screens.ExamScreen
import com.edukrd.app.ui.screens.ForgotPasswordScreen
import com.edukrd.app.ui.screens.HomeScreen
import com.edukrd.app.ui.screens.LoginScreen
import com.edukrd.app.ui.screens.MedalsScreen
import com.edukrd.app.ui.screens.OnboardingScreen
import com.edukrd.app.ui.screens.RankingScreen
import com.edukrd.app.ui.screens.RegisterScreen
import com.edukrd.app.ui.screens.SettingsScreen
import com.edukrd.app.ui.screens.StoreScreen
import com.edukrd.app.ui.screens.VerificationPendingScreen
import com.edukrd.app.viewmodel.OnboardingViewModel
import com.edukrd.app.viewmodel.ThemeViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    themeViewModel: ThemeViewModel
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }
        composable(route = "onboarding") {
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            OnboardingScreen(navController = navController, onboardingViewModel = onboardingViewModel)
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
        composable(
            route = Screen.VerificationPending.route,
            arguments = listOf(navArgument("email") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            VerificationPendingScreen(navController, email)
        }
    }
}
