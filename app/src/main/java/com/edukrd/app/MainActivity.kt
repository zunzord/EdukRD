package com.edukrd.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.edukrd.app.navigation.NavGraph
import com.edukrd.app.ui.theme.EdukRDTheme
import com.edukrd.app.viewmodel.AuthViewModel
import com.edukrd.app.viewmodel.OnboardingViewModel
import com.edukrd.app.viewmodel.ThemeViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            val themePreference by themeViewModel.themePreference.collectAsState()
            val uid by authViewModel.uid.collectAsState()
            val isOnboardingCompleted by onboardingViewModel.isOnboardingCompleted.collectAsState()
            val navController = rememberNavController()
            LaunchedEffect(isOnboardingCompleted) {
                if (!isOnboardingCompleted) {
                    navController.navigate("onboarding") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
            LaunchedEffect(uid, isOnboardingCompleted) {
                if (isOnboardingCompleted) {
                    if (uid != null) {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        firebaseUser?.reload()?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                if (firebaseUser.isEmailVerified) {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("verification_pending") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            } else {
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        }
                    } else {
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                }
            }
            EdukRDTheme(darkTheme = (themePreference == "dark")) {
                NavGraph(
                    navController = navController,
                    startDestination = "login",
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}
