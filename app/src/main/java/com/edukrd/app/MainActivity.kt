package com.edukrd.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.edukrd.app.navigation.NavGraph
import com.edukrd.app.navigation.Screen
import com.edukrd.app.ui.theme.EdukRDTheme
import com.edukrd.app.viewmodel.AuthViewModel
import com.edukrd.app.viewmodel.ThemeViewModel
import com.edukrd.app.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val userViewModel: UserViewModel = hiltViewModel()

            val themePreference by themeViewModel.themePreference.collectAsState()
            val uid by authViewModel.uid.collectAsState()
            val navigationCommand by userViewModel.navigationCommand.collectAsState(initial = null)

            val navController = rememberNavController()

            // Cuando cambia la autenticación, cargamos datos si está logueado
            LaunchedEffect(uid) {
                if (uid != null) {
                    userViewModel.loadCurrentUserData()
                } else {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            }

            // Navegación reactiva según el comando emitido por UserViewModel
            LaunchedEffect(navigationCommand) {
                when (navigationCommand) {
                    UserViewModel.NavigationCommand.ToOnboarding -> {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                    UserViewModel.NavigationCommand.ToHome -> {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                    UserViewModel.NavigationCommand.ContactSupport -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Error cargando perfil. Contacta soporte.",
                            Toast.LENGTH_LONG
                        ).show()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                    null -> Unit
                }
            }

            EdukRDTheme(darkTheme = (themePreference == "dark")) {
                NavGraph(
                    navController = navController,
                    startDestination = Screen.Login.route,
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}
