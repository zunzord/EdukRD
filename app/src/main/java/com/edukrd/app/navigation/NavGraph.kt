package com.edukrd.app.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.edukrd.app.ui.screens.CourseScreen
import com.edukrd.app.ui.screens.ErrorScreen
import com.edukrd.app.ui.screens.ExamResultScreen
import com.edukrd.app.ui.screens.ExamScreen
import com.edukrd.app.ui.screens.ForgotPasswordScreen
import com.edukrd.app.ui.screens.LoginScreen
import com.edukrd.app.ui.screens.MedalsScreen
import com.edukrd.app.ui.screens.OnboardingScreen
import com.edukrd.app.ui.screens.RankingScreen
import com.edukrd.app.ui.screens.RegisterScreen
import com.edukrd.app.ui.screens.SessionValidationScreen
import com.edukrd.app.ui.screens.SettingsScreen
import com.edukrd.app.ui.screens.SplashScreen
import com.edukrd.app.ui.screens.StoreScreen
import com.edukrd.app.ui.screens.VerificationPendingScreen
import com.edukrd.app.ui.screens.home.HomeScreen
import com.edukrd.app.viewmodel.OnboardingViewModel
import com.edukrd.app.viewmodel.ThemeViewModel


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    themeViewModel: ThemeViewModel,
    navMedallasOffset: Offset,
    navTiendaOffset: Offset,
    navRankingOffset: Offset
) {
    // Observa el back stack actual para disparar la animación en cada cambio
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    AnimatedContent(
        targetState = currentBackStackEntry,
        transitionSpec = {
            // Define una transición compuesta: desliza y desvanecimiento
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 500)
            ) + fadeIn(animationSpec = tween(durationMillis = 500)) with
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeOut(animationSpec = tween(durationMillis = 500))
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
                HomeScreen(navController,navMedallasOffset = navMedallasOffset,navTiendaOffset = navTiendaOffset,navRankingOffset = navRankingOffset)
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

            composable(
                route = Screen.ExamResultScreen.route,
                arguments = listOf(
                    navArgument("courseId") { type = NavType.StringType },
                    navArgument("finalScore") { type = NavType.IntType },
                    navArgument("correctCount") { type = NavType.IntType },
                    navArgument("totalQuestions") { type = NavType.IntType },
                    navArgument("passed") { type = NavType.BoolType },
                    navArgument("coinsEarned") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                val finalScore = backStackEntry.arguments?.getInt("finalScore") ?: 0
                val correctCount = backStackEntry.arguments?.getInt("correctCount") ?: 0
                val totalQuestions = backStackEntry.arguments?.getInt("totalQuestions") ?: 0
                val passed = backStackEntry.arguments?.getBoolean("passed") ?: false
                val coinsEarned = backStackEntry.arguments?.getInt("coinsEarned") ?: 0

                ExamResultScreen(
                    courseId = courseId,
                    finalScore = finalScore,
                    correctCount = correctCount,
                    totalQuestions = totalQuestions,
                    passed = passed,
                    coinsEarned = coinsEarned,
                    onContinue = {
                        // Navegar al HomeScreen
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onRetry = {
                        // Navegar de vuelta a ExamScreen para reintentar el examen
                        navController.navigate(Screen.Exam.createRoute(courseId))
                    }
                )
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
            composable("session_validation") {
                SessionValidationScreen(navController = navController)
            }
        }
    }
}
