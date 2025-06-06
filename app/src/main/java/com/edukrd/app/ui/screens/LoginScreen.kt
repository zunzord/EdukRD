package com.edukrd.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.R
import com.edukrd.app.ui.components.DotLoadingIndicator
import com.edukrd.app.ui.components.UnderlinedTextField
import com.edukrd.app.viewmodel.AuthViewModel
import com.edukrd.app.viewmodel.SessionViewModel
import com.edukrd.app.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showActiveSessionDialog by remember { mutableStateOf(false) }

    // Observa los comandos de navegación emitidos por UserViewModel
    val navigationCommand by userViewModel.navigationCommand.collectAsState(initial = null)

    LaunchedEffect(navigationCommand) {
        when (navigationCommand) {
            UserViewModel.NavigationCommand.ToOnboarding -> navController.navigate("onboarding") {
                popUpTo("login") { inclusive = true }
            }
            UserViewModel.NavigationCommand.ToHome -> navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
            UserViewModel.NavigationCommand.ContactSupport -> {
                Toast.makeText(context, "Error cargando perfil. Contacta soporte.", Toast.LENGTH_LONG).show()
                navController.navigate("login") {
                    popUpTo("login") { inclusive = true }
                }
            }
            null -> Unit
        }
    }

    // Manejo del resultado de la autenticación (login o registro)
    LaunchedEffect(authViewModel) {
        authViewModel.authResult.collect { result ->
            when (result) {
                is com.edukrd.app.viewmodel.AuthResult.Success -> {
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    // Validación de email verificado (esta parte permanece sin cambios)
                    if (firebaseUser != null && !firebaseUser.isEmailVerified) {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("verification_pending") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        // Cargamos los datos del usuario, manteniendo la lógica actual.
                        userViewModel.loadCurrentUserData()
                        // Una vez cumplidas las validaciones (auth, email y datos del usuario),
                        // se procede a gestionar la sesión robusta.
                        //sessionViewModel.createNewSession()
                    }
                    isLoading = false
                }
                is com.edukrd.app.viewmodel.AuthResult.Error -> {
                    errorMessage = result.message
                    isLoading = false
                }
            }
        }
    }

    // Observa el canal de conflicto de sesión para mostrar el diálogo cuando sea necesario.
   /* LaunchedEffect(key1 = sessionViewModel) {
        sessionViewModel.sessionConflictEvent.collect { conflictMessage ->
            showActiveSessionDialog = true
            Toast.makeText(context, conflictMessage, Toast.LENGTH_LONG).show()
        }
    }*/

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo Edukrd",
                modifier = Modifier
                    .width(280.dp)
                    .height(280.dp)
                    .padding(bottom = 24.dp)
            )
            UnderlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = "Correo electrónico",
                modifier = Modifier.fillMaxWidth(),

            )
            Spacer(modifier = Modifier.height(8.dp))
            UnderlinedTextField(
                value = password,
                onValueChange = { password = it.trim() },
                label = "Contraseña",
                modifier = Modifier.fillMaxWidth(),
                isPassword = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Por favor ingresa correo y contraseña."
                    } else {
                        isLoading = true
                        errorMessage = ""
                        authViewModel.login(email, password)
                    }
                },
                modifier = Modifier
                    .width(150.dp)
                    .height(40.dp),
                enabled = !isLoading
            ) {
                if (isLoading) DotLoadingIndicator(modifier = Modifier.size(56.dp))
                else Text("Ingresar")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { navController.navigate("forgot_password") }) {
                Text("¿Olvidaste tu contraseña?")
            }
            TextButton(onClick = { navController.navigate("register") }) {
                Text("¿No tienes cuenta? Regístrate")
            }
        }
    }

    // Diálogo que se muestra si se detecta que hay sesión activa en otro dispositivo.
    if (showActiveSessionDialog) {
        ActiveSessionDialog(
            onConfirm = {
                // El usuario opta por cerrar la sesión previa.
                sessionViewModel.closeSession()
                showActiveSessionDialog = false
                Toast.makeText(context, "Se ha cerrado la sesión anterior. Iniciando nueva sesión.", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                // El usuario cancela la acción, se evita continuar con el login.
                showActiveSessionDialog = false
                authViewModel.logout() // Se invoca el logout para limpiar el estado.
            }
        )
    }
}

@Composable
fun ActiveSessionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sesión activa detectada") },
        text = { Text("Ya hay una sesión activa en otro dispositivo. ¿Deseas cerrar la sesión anterior y continuar?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Sí, cerrar sesión anterior")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
