package com.edukrd.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.hilt.navigation.compose.hiltViewModel
import com.edukrd.app.navigation.Screen
import com.edukrd.app.viewmodel.SplashViewModel
import com.edukrd.app.ui.components.DotLoadingIndicator

@Composable
fun SplashScreen(navController: NavHostController) {
    val viewModel: SplashViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.checkSession { loggedIn ->
            navController.navigate(if (loggedIn) Screen.Home.route else Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        DotLoadingIndicator(modifier = Modifier.size(32.dp))
    }
}
