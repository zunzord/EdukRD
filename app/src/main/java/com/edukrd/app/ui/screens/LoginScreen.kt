package com.edukrd.app.ui.screens

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.edukrd.app.utils.showToast
import com.edukrd.app.utils.isInternetAvailable
import com.edukrd.app.R
import androidx.compose.ui.layout.ContentScale


@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Logo de la app",
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(bottom = 24.dp),
            contentScale = ContentScale.Fit
        )


        Spacer(modifier = Modifier.height(20.dp))


        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))


        OutlinedTextField(
            value = password,
            onValueChange = { password = it.trim() },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(modifier = Modifier.height(30.dp))


        Button(
            onClick = {
                when {
                    email.isEmpty() || password.isEmpty() -> {
                        showToast(context, "Ingresa correo y contraseña")
                    }
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        showToast(context, "El correo electrónico no es válido")
                    }
                    !isInternetAvailable(context) -> {
                        showToast(context, "No tienes conexión a internet")
                    }
                    else -> {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    if (user != null && user.isEmailVerified) {
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        showToast(context, "Verifica tu correo antes de iniciar sesión")
                                    }
                                } else {
                                    showToast(context, "Error: ${task.exception?.message}")
                                }
                            }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF264c73)) // Color azul oscuro
        ) {
            Text(text = "Iniciar sesión", fontSize = 18.sp, color = Color.White)
        }


        Spacer(modifier = Modifier.height(20.dp))


        TextButton(onClick = { navController.navigate("forgot_password") }) {
            Text(text = "¿Olvidaste tu contraseña?", fontSize = 14.sp, color = Color(0xFF264c73))
        }

        Spacer(modifier = Modifier.height(8.dp))


        TextButton(onClick = { navController.navigate("register") }) {
            Text(text = "¿No tienes cuenta? Regístrate", fontSize = 14.sp, color = Color(0xFF264c73))
        }
    }
}
