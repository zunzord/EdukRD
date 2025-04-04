package com.edukrd.app.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.edukrd.app.ui.screens.*
import com.edukrd.app.ui.screens.home.HomeScreen
import com.edukrd.app.viewmodel.OnboardingViewModel
import com.edukrd.app.viewmodel.ThemeViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    themeViewModel: ThemeViewModel
) {
    // Observa el back stack actual para disparar la animación en cada cambio
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    AnimatedContent(
        targetState = currentBackStackEntry,
        transitionSpec = {
            // Define una transición compuesta: desliza y desvanecimiento
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)) with
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
        }
    ) { targetState ->

        targetState.let { }
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(navController)
            }
            composable(Screen.Login.route) {
                LoginScreen(navController)
            }
            composable(Screen.Register.route) {
                RegisterScreen(navController)
            }
            composable(Screen.Onboarding.route) {
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
                if (courseId.isNullOrEmpty()) ErrorScreen(navController)
                else CourseScreen(navController, courseId)
            }
            composable(Screen.Exam.route) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId")
                if (courseId.isNullOrEmpty()) ErrorScreen(navController)
                else ExamScreen(navController, courseId)
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
}
