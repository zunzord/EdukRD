package com.edukrd.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.viewmodel.AuthResult
import com.edukrd.app.viewmodel.AuthViewModel
import com.edukrd.app.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Observe navigation commands from UserViewModel
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

    LaunchedEffect(authViewModel) {
        authViewModel.authResult.collect { result ->
            when (result) {
                is AuthResult.Success -> {
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null && !firebaseUser.isEmailVerified) {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("verification_pending") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        userViewModel.loadCurrentUserData()
                    }
                    isLoading = false
                }
                is AuthResult.Error -> {
                    errorMessage = result.message
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Iniciar Sesión") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.trim() },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
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
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
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
}
