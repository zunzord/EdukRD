package com.edukrd.app.ui.screens

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.edukrd.app.utils.showToast
import com.edukrd.app.utils.isInternetAvailable

@Composable
fun RegisterScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Registro", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = name, onValueChange = { name = it.trim() }, label = { Text("Nombre") })
        OutlinedTextField(value = lastName, onValueChange = { lastName = it.trim() }, label = { Text("Apellido") })
        OutlinedTextField(value = birthDate, onValueChange = { birthDate = it.trim() }, label = { Text("Fecha de Nacimiento (DD/MM/AAAA)") })
        OutlinedTextField(value = sector, onValueChange = { sector = it.trim() }, label = { Text("Sector") })
        OutlinedTextField(value = phone, onValueChange = { phone = it.trim() }, label = { Text("Teléfono") })
        OutlinedTextField(value = email, onValueChange = { email = it.trim() }, label = { Text("Email") })
        OutlinedTextField(value = password, onValueChange = { password = it.trim() }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it.trim() }, label = { Text("Confirmar Contraseña") }, visualTransformation = PasswordVisualTransformation())

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            when {
                !isInternetAvailable(context) -> {
                    showToast(context, "No hay conexión a Internet")
                }
                name.isEmpty() || lastName.isEmpty() || birthDate.isEmpty() || sector.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                    showToast(context, "Todos los campos son obligatorios")
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showToast(context, "El correo electrónico no es válido")
                }
                password != confirmPassword -> {
                    showToast(context, "Las contraseñas no coinciden")
                }
                else -> {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val userId = user?.uid

                                if (userId != null) {
                                    val userData = hashMapOf(
                                        "name" to name,
                                        "lastName" to lastName,
                                        "birthDate" to birthDate,
                                        "sector" to sector,
                                        "phone" to phone,
                                        "email" to email,
                                        "createdAt" to FieldValue.serverTimestamp()
                                    )

                                    db.collection("users").document(userId)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            user.sendEmailVerification().addOnCompleteListener { verificationTask ->
                                                if (verificationTask.isSuccessful) {
                                                    showToast(context, "Verifica tu correo antes de iniciar sesión")
                                                    navController.navigate("login") {
                                                        popUpTo("register") { inclusive = true }
                                                    }
                                                } else {
                                                    showToast(context, "Error al enviar correo de verificación")
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            showToast(context, "Error al guardar datos en Firestore")
                                        }
                                }
                            } else {
                                showToast(context, "Error: ${task.exception?.message}")
                            }
                        }
                }
            }
        }) {
            Text(text = "Registrarse")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { navController.navigate("login") }) {
            Text(text = "¿Ya tienes cuenta? Inicia sesión")
        }
    }
}
