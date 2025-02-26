package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.edukrd.app.models.Course
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun MainHomeScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val userId = auth.currentUser?.uid
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                // 🔹 Obtener datos del usuario
                val userSnapshot = db.collection("users").document(userId).get().await()
                if (userSnapshot.exists()) {
                    userData = userSnapshot.data
                } else {
                    errorMessage = "No se encontraron datos del usuario."
                }

                // 🔹 Obtener cursos disponibles (sin cargar contenido aún)
                val coursesSnapshot = db.collection("courses").get().await()
                val coursesList = coursesSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Course::class.java)?.copy(id = doc.id)
                }

                Log.d("TEST_FIRESTORE", "Cursos obtenidos: $coursesList")
                courses = coursesList
                loading = false
            } catch (e: Exception) {
                Log.e("MainHomeScreen", "Error al obtener datos", e)
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
        } else {
            // 🔹 Mostrar datos del usuario
            Text(text = "Bienvenido, ${userData?.get("name") ?: "Usuario"}", style = MaterialTheme.typography.h4)
            Spacer(modifier = Modifier.height(8.dp))

            // 🔹 Mostrar lista de cursos disponibles
            Text(text = "Cursos disponibles:", style = MaterialTheme.typography.h6)
            LazyColumn {
                items(courses) { course ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { navigateToCourse(userId, course, db, navController) },
                        elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = course.title, style = MaterialTheme.typography.h6)
                            Text(text = course.description, style = MaterialTheme.typography.body2)
                        }
                    }
                }
            }

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

// 🔹 Función para manejar la navegación al curso
fun navigateToCourse(userId: String?, course: Course, db: FirebaseFirestore, navController: NavController) {
    if (userId == null) return

    val progressRef = db.collection("progress").document(userId).collection("courses").document(course.id)
    progressRef.get().addOnSuccessListener { document ->
        if (!document.exists()) {
            // 🔹 Si el usuario aún no tiene progreso, crearlo antes de navegar
            val progressData = mapOf(
                "courseId" to course.id,
                "progressPercentage" to 0,
                "completed" to false,
                "lastAccessed" to System.currentTimeMillis()
            )
            progressRef.set(progressData).addOnSuccessListener {
                Log.d("TEST_FIRESTORE", "Curso iniciado: ${course.title}")
            }.addOnFailureListener { e ->
                Log.e("TEST_FIRESTORE", "Error al iniciar curso", e)
            }
        }
        // 🔹 Navegar al curso sin esperar la creación del progreso
        navController.navigate("course/${course.id}")
    }.addOnFailureListener { e ->
        Log.e("TEST_FIRESTORE", "Error al verificar progreso del curso", e)
    }
}
