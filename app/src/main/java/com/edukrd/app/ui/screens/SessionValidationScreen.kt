package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.edukrd.app.R
import com.edukrd.app.viewmodel.SessionViewModel
import kotlinx.coroutines.delay

@Composable
fun SessionValidationScreen(navController: NavHostController) {
    // Inyectamos el SessionViewModel
    val sessionViewModel: SessionViewModel = hiltViewModel()
    // Recogemos el estado de la sesión
    val sessionState by sessionViewModel.sessionState.collectAsState(initial = null)
    // Asignamos a una variable local para usarlo con smart cast
    val currentSession = sessionState

    // Registra en el log el valor recibido del ViewModel.
    Log.d("SessionValidationScreen", "Received session: $currentSession")

    // Pantalla de carga (puedes personalizarla según necesites)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Logo Edukrd"
        )
        CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        Text(
            text = "Validando sesión...",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    // Efecto que espera un breve lapso y luego decide la navegación según la sesión.
    LaunchedEffect(currentSession) {
        Log.d("SessionValidationScreen", "Validating session in 3 seconds...")
        delay(3000)
        Log.d("SessionValidationScreen", "Finished waiting, evaluating session: $currentSession")
        if (currentSession != null && currentSession.active) {
            Log.d("SessionValidationScreen", "Session is active. Navigating to home screen.")
            navController.navigate("home") {
                popUpTo("session_validation") { inclusive = true }
            }
        } else {
            Log.d("SessionValidationScreen", "Session is invalid or null. Navigating to login screen.")
            navController.navigate("login") {
                popUpTo("session_validation") { inclusive = true }
            }
        }
    }
}
