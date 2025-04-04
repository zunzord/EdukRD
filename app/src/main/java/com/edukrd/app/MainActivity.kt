package com.edukrd.app

import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.edukrd.app.ui.components.MainScaffold
import com.edukrd.app.ui.theme.EdukRDTheme
import com.edukrd.app.viewmodel.AuthResult
import com.edukrd.app.viewmodel.AuthViewModel
import com.edukrd.app.viewmodel.ThemeViewModel
import com.edukrd.app.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Solicita el permiso de notificaciones al iniciar
            RequestNotificationPermission()

            val themeViewModel: ThemeViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val userViewModel: UserViewModel = hiltViewModel()

            val themePreference by themeViewModel.themePreference.collectAsState()
            val authResult by authViewModel.authResult.collectAsState(initial = null)

            LaunchedEffect(authResult) {
                if (authResult is AuthResult.Success) {
                    userViewModel.loadCurrentUserData()
                }
            }

            EdukRDTheme(darkTheme = (themePreference == "dark")) {
                // contenedor para barra de navegacion inferior persistente
                MainScaffold()
            }
        }
    }
}

@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current

    // Define el launcher para solicitar el permiso
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.d("Permissions", "POST_NOTIFICATIONS permission granted")
            } else {
                Log.d("Permissions", "POST_NOTIFICATIONS permission denied")
            }
        }
    )

    // valida y solicita el permiso
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
