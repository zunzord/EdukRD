package com.edukrd.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.edukrd.app.navigation.NavGraph
import com.edukrd.app.navigation.Screen
import com.edukrd.app.ui.screens.home.components.BottomNavigationBar
import com.edukrd.app.viewmodel.ThemeViewModel

@Composable
fun MainScaffold(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val themeViewModel: ThemeViewModel = hiltViewModel()
    // Observa el backStack para determinar la ruta actual.
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    // Si currentBackStackEntry es nulo, usamos cadena vacía para evitar mostrar la barra
    val route = currentBackStackEntry?.destination?.route ?: ""

    // Definimos las rutas donde NO se debe mostrar la barra inferior.
    val routesWithoutBottomBar = listOf(
        Screen.Login.route,
        Screen.Register.route,
        Screen.ForgotPassword.route,
        Screen.Onboarding.route,
        Screen.Splash.route,
        Screen.VerificationPending.route,
        Screen.Course.route,
        Screen.Exam.route
    )
    // La barra se muestra solo si la ruta actual no es una de las excluidas y no es cadena vacía.
    val shouldShowBottomBar = route.isNotEmpty() && route !in routesWithoutBottomBar

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AnimatedVisibility(visible = shouldShowBottomBar) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = route,
                    onMedalsIconPosition = {},
                    onStoreIconPosition = {},
                    onRankingIconPosition = {}
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavGraph(
                navController = navController,
                startDestination = Screen.Splash.route,
                themeViewModel = themeViewModel
            )
        }
    }
}
