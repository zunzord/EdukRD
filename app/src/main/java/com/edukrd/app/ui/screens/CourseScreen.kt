package com.edukrd.app.ui.screens

import android.util.Log
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import coil.compose.AsyncImage

@Composable
fun CourseScreen(navController: NavController, courseId: String) {
    val db = FirebaseFirestore.getInstance()

    var course by remember { mutableStateOf<Course?>(null) }
    var contentList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(courseId) {
        try {
            // 🔹 Obtener datos generales del curso
            val courseDoc = db.collection("courses").document(courseId).get().await()
            if (courseDoc.exists()) {
                course = courseDoc.toObject(Course::class.java)?.copy(id = courseId)

                // 🔹 Obtener contenido del curso desde la subcolección "content"
                val contentSnapshot = db.collection("courses")
                    .document(courseId)
                    .collection("content")
                    .orderBy("order") // 🔹 Ordenamos por número de página
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
            course?.let {
                Text(text = it.title, style = MaterialTheme.typography.h4)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it.description, style = MaterialTheme.typography.body1)
                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Mostrar contenido del curso
                if (contentList.isNotEmpty()) {
                    Text(text = "Contenido del curso:", style = MaterialTheme.typography.h6)
                    LazyColumn {
                        items(contentList) { page ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                elevation = 4.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    val title = page["title"] as? String ?: "Sin título"
                                    val text = page["text"] as? String ?: "Sin contenido"
                                    val imageUrl = page["imageUrl"] as? String

                                    // ✅ Mostrar imagen si existe
                                    if (!imageUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = "Imagen del contenido",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                        )
                                    }

                                    Text(text = title, style = MaterialTheme.typography.h6)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = text, style = MaterialTheme.typography.body2)
                                }
                            }
                        }
                    }
                } else {
                    Text(text = "No hay contenido disponible para este curso.", style = MaterialTheme.typography.body2)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { navController.popBackStack() }) {
                    Text(text = "Volver")
                }
            }
        }
    }
}
