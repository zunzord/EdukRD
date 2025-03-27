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
import com.edukrd.app.viewmodel.AuthResult
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
            val authResult by authViewModel.authResult.collectAsState(initial = null)
            val navigationCommand by userViewModel.navigationCommand.collectAsState(initial = null)

            val navController = rememberNavController()

            LaunchedEffect(authResult) {
                if (authResult is AuthResult.Success) {
                    userViewModel.loadCurrentUserData()
                }
            }



            EdukRDTheme(darkTheme = (themePreference == "dark")) {
                NavGraph(navController = navController, startDestination = Screen.Splash.route, themeViewModel = themeViewModel)
            }
        }
    }
}
