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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.models.User
import com.edukrd.app.viewmodel.AuthResult
import com.edukrd.app.viewmodel.AuthViewModel
import com.edukrd.app.viewmodel.UserViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val context = LocalContext.current

    // Campos de registro
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var themePreference by remember { mutableStateOf("light") } // Preferencia de tema por defecto

    // Observa los resultados de autenticación (login/register)
    LaunchedEffect(Unit) {
        authViewModel.authResult.collect { result ->
            when (result) {
                is AuthResult.Success -> {
                    // Registro exitoso en FirebaseAuth
                    // Construir objeto User con los datos ingresados
                    val newUser = User(
                        name = name,
                        lastName = lastName,
                        birthDate = birthDate,
                        sector = sector,
                        phone = phone,
                        email = email,
                        notificationsEnabled = false,
                        notificationFrequency = "Diaria",
                        themePreference = themePreference
                    )

                    // Actualizar datos adicionales en Firestore (UserViewModel obtiene UID internamente)
                    userViewModel.updateCurrentUserData(newUser) { updateSuccess ->
                        if (updateSuccess) {
                            // Enviar correo de verificación
                            val currentUser = authViewModel.uid.value
                            // OJO: En la práctica, usar FirebaseAuth.getInstance().currentUser?.sendEmailVerification()
                            // si deseas enviar verificación con la sesión actual.
                            // Ejemplo:
                            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            firebaseUser?.sendEmailVerification()?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "Verifica tu correo antes de iniciar sesión",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    navController.navigate("login") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Error al enviar correo de verificación",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Error al guardar datos del usuario", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                is AuthResult.Error -> {
                    // Error al registrar en FirebaseAuth
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Registro", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.trim() },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it.trim() },
                label = { Text("Apellido") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it.trim() },
                label = { Text("Fecha de Nacimiento (DD/MM/AAAA)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = sector,
                onValueChange = { sector = it.trim() },
                label = { Text("Sector") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.trim() },
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.trim() },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it.trim() },
                label = { Text("Confirmar Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Preferencia de Tema", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = (themePreference == "light"),
                    onClick = { themePreference = "light" }
                )
                Text("Claro")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = (themePreference == "dark"),
                    onClick = { themePreference = "dark" }
                )
                Text("Oscuro")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    when {
                        name.isEmpty() || lastName.isEmpty() || birthDate.isEmpty() ||
                                sector.isEmpty() || phone.isEmpty() || email.isEmpty() ||
                                password.isEmpty() || confirmPassword.isEmpty() -> {
                            Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_LONG).show()
                        }
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                            Toast.makeText(context, "El correo electrónico no es válido", Toast.LENGTH_LONG).show()
                        }
                        password != confirmPassword -> {
                            Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            // Realiza el registro en FirebaseAuth
                            authViewModel.register(email, password)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registrarse")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { navController.navigate("login") }) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }
        }
    }
}
