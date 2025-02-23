package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val userId = auth.currentUser?.uid
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData = document.data
                        loading = false
                    } else {
                        errorMessage = "No se encontraron datos del usuario."
                        loading = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HomeScreen", "Error al obtener datos del usuario", e)
                    errorMessage = "Error al obtener datos."
                    loading = false
                }
        } else {
            errorMessage = "Usuario no autenticado."
            loading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colors.error)
        } else if (userData != null) {
            Text(text = "Bienvenido, ${userData?.get("name") ?: "Usuario"}", style = MaterialTheme.typography.h4)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Apellido: ${userData?.get("lastName") ?: "No registrado"}")
            Text(text = "Correo: ${userData?.get("email") ?: "No registrado"}")
            Text(text = "Fecha de Nacimiento: ${userData?.get("birthDate") ?: "No registrado"}")
            Text(text = "Sector: ${userData?.get("sector") ?: "No registrado"}")
            Text(text = "Teléfono: ${userData?.get("phone") ?: "No registrado"}")

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                auth.signOut()
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }) {
                Text(text = "Cerrar sesión")
            }
        }
    }
}
