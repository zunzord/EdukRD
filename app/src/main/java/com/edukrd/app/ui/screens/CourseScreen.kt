package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack // Usamos Icons.Filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.edukrd.app.models.Course
import com.edukrd.app.navigation.AppScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(navController: NavController, courseId: String) {
    val dominicanBlue = Color(0xFF1565C0)
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    var course by remember { mutableStateOf<Course?>(null) }
    var contentList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(courseId) {
        try {
            val courseDoc = db.collection("courses").document(courseId).get().await()
            if (courseDoc.exists()) {
                course = courseDoc.toObject(Course::class.java)?.copy(id = courseId)

                val contentSnapshot = db.collection("courses")
                    .document(courseId)
                    .collection("content")
                    .orderBy("order")
                    .get()
                    .await()

                contentList = contentSnapshot.documents.mapNotNull { it.data }
            } else {
                errorMessage = "Curso no encontrado"
            }
            loading = false
        } catch (e: Exception) {
            Log.e("CourseScreen", "Error al obtener el curso", e)
            errorMessage = "Error al cargar el curso"
            loading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(16.dp),
                containerColor = dominicanBlue
            ) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (loading) {
                CircularProgressIndicator()
            } else if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            } else {
                course?.let {
                    Text(
                        text = it.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = dominicanBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it.description, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn {
                        items(contentList) { page ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    AsyncImage(
                                        model = page["imageUrl"] as? String,
                                        contentDescription = "Imagen del curso",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = page["text"] as? String ?: "Sin contenido",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }


                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (userId != null) {
                                        navController.navigate(
                                            AppScreen.Exam.createRoute(userId, courseId)
                                        )
                                    } else {
                                        Log.e("CourseScreen", "Error: Usuario no autenticado")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = userId != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = dominicanBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(text = "Tomar Examen")
                            }
                        }
                    }
                }
            }
        }
    }
}
