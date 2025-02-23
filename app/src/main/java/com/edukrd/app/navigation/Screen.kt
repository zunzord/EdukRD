package com.edukrd.app.navigation

sealed class Screen(val route: String) {
    object LoginScreen : Screen("login")
    object RegisterScreen : Screen("register")
    object ForgotPasswordScreen : Screen("forgot_password")
    object HomeScreen : Screen("home")
}
