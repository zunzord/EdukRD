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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.R
import com.edukrd.app.viewmodel.AuthResult
import com.edukrd.app.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }

    // Escucha el flujo de resultados de autenticación para la acción de envío de correo de recuperación.
    LaunchedEffect(Unit) {
        authViewModel.authResult.collect { result ->
            when (result) {
                is AuthResult.Success -> {
                    Toast.makeText(
                        context,
                        "Correo de recuperación enviado",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.navigate("login") {
                        popUpTo("forgot_password") { inclusive = true }
                    }
                }
                is AuthResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

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
                painter = painterResource(id = R.drawable.recoverpassword),
                contentDescription = "Imagen de cabecera",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(), // Permite que la imagen tenga la altura que necesite
                contentScale = ContentScale.Fit,
                alignment = Alignment.TopStart
            )
            Text(text = "Recuperar contraseña", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Correo electrónico") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (email.isEmpty()) {
                        Toast.makeText(context, "Ingrese un correo válido", Toast.LENGTH_LONG).show()
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(context, "El formato del correo no es válido", Toast.LENGTH_LONG).show()
                    } else {
                        authViewModel.sendPasswordReset(email)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar correo")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { navController.navigate("login") }) {
                Text("Volver al login")
            }
        }
    }
}
