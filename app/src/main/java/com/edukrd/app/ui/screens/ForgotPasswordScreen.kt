package com.edukrd.app.ui.screens

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.edukrd.app.utils.showToast
import com.edukrd.app.utils.isInternetAvailable

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Recuperar contraseña", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("Correo electrónico") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            when {
                email.isEmpty() -> {
                    showToast(context, "Ingrese un correo válido")
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showToast(context, "El formato del correo no es válido")
                }
                !isInternetAvailable(context) -> {  // 🔹 Verifica conexión antes de continuar
                    showToast(context, "Sin conexión a Internet. Intente nuevamente.")
                }
                else -> {
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                showToast(context, "Correo de recuperación enviado")
                                navController.navigate("login") {
                                    popUpTo("forgot_password") { inclusive = true }
                                }
                            } else {
                                showToast(context, "Error: ${task.exception?.message}")
                            }
                        }
                }
            }
        }) {
            Text(text = "Enviar correo")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { navController.navigate("login") }) {
            Text(text = "Volver al login")
        }
    }
}
