package com.edukrd.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.edukrd.app.navigation.NavGraph
import com.edukrd.app.ui.theme.EdukRDTheme
import com.edukrd.app.viewmodel.AuthViewModel
import com.edukrd.app.viewmodel.ThemeViewModel
import com.google.firebase.auth.FirebaseAuth
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

            // Observamos el tema y el UID de la sesión actual
            val themePreference by themeViewModel.themePreference.collectAsState()
            val uid by authViewModel.uid.collectAsState()

            val navController = rememberNavController()

            // Al detectar un cambio en uid, se recarga el usuario actual para actualizar su estado de verificación
            LaunchedEffect(uid) {
                if (uid != null) {
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    firebaseUser?.reload()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if (firebaseUser.isEmailVerified) {
                                // Usuario verificado: navegar al Home
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                // Usuario no verificado: redirigir a la pantalla de verificación pendiente
                                navController.navigate("verification_pending") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        } else {
                            // Si falla la recarga, se redirige a login (o se podría manejar el error de otra forma)
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }
                } else {
                    // Si no hay usuario autenticado, ir a login
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            }

            // Aplicamos el tema según la preferencia del usuario
            EdukRDTheme(darkTheme = (themePreference == "dark")) {
                // Se define un startDestination por defecto ("login"), ya que la navegación se controlará desde LaunchedEffect
                NavGraph(
                    navController = navController,
                    startDestination = "login",
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}
