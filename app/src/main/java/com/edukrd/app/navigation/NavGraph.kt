package com.edukrd.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.edukrd.app.ui.screens.LoginScreen
import com.edukrd.app.ui.screens.RegisterScreen
import com.edukrd.app.ui.screens.ForgotPasswordScreen
import com.edukrd.app.ui.screens.HomeScreen

sealed class AppScreen(val route: String) {
    object Login : AppScreen("login")
    object Register : AppScreen("register")
    object ForgotPassword : AppScreen("forgot_password")
    object Home : AppScreen("home")
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
        composable(AppScreen.Home.route) { HomeScreen(navController) }

    }
}
