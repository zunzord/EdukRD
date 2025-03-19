package com.edukrd.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.edukrd.app.navigation.NavGraph
import com.edukrd.app.ui.theme.EdukRDTheme
import com.edukrd.app.viewmodel.AuthViewModel
import com.edukrd.app.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Inyectamos ThemeViewModel y AuthViewModel
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()

            // Observamos el tema y la sesión de forma reactiva
            val themePreference by themeViewModel.themePreference.collectAsState()
            val uid by authViewModel.uid.collectAsState()

            // Determinamos la pantalla inicial según si hay sesión activa
            val startDestination = if (uid != null) "home" else "login"

            // Aplicamos el tema según el valor actualizado de themePreference
            EdukRDTheme(darkTheme = (themePreference == "dark")) {
                val navController = rememberNavController()

                // Pasamos la misma instancia de ThemeViewModel a NavGraph
                NavGraph(
                    navController = navController,
                    startDestination = startDestination,
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}
