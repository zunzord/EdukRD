package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.CircleCropTransformation
import com.edukrd.app.R
import com.edukrd.app.models.Course
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt
import androidx.compose.material3.Card
import androidx.compose.ui.platform.LocalContext

@Composable
fun MainHomeScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    var userName by remember { mutableStateOf("Usuario") }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // gráfico de pastel
    var completedCourses by remember { mutableStateOf(0) }
    var totalCourses by remember { mutableStateOf(0) }

    // cursos aprobado
    var passedCoursesSet by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(userId) {
        if (userId == null) {
            errorMessage = "Usuario no autenticado."
            loading = false
            return@LaunchedEffect
        }

        try {
            // Datos de usuario
            val userSnapshot = db.collection("users").document(userId).get().await()
            if (userSnapshot.exists()) {
                userName = userSnapshot.getString("name") ?: "Usuario"
            } else {
                errorMessage = "No se encontraron datos del usuario."
            }

            // Cursos totales
            val coursesSnapshot = db.collection("courses").get().await()
            val coursesList = coursesSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Course::class.java)?.copy(id = doc.id)
            }
            courses = coursesList
            totalCourses = coursesList.size

            // Cursos aprobados
            val passedDocs = db.collection("examResults")
                .whereEqualTo("userId", userId)
                .whereEqualTo("passed", true)
                .get().await()
            val passedCourses = passedDocs.documents.mapNotNull { it.getString("courseId") }.toSet()
            completedCourses = passedCourses.size

            passedCoursesSet = passedCourses

            loading = false
        } catch (e: Exception) {
            Log.e("MainHomeScreen", "Error al obtener datos", e)
            errorMessage = "Error al obtener datos."
            loading = false
        }
    }

    Scaffold { innerPadding ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Sección gráfico
                item {
                    HeaderWithPieChart(
                        userName = userName,
                        completedCourses = completedCourses,
                        totalCourses = totalCourses
                    )
                }

                // Opciones principales
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    DashboardOptionsSection(navController)

                }

                // Lista de cursos
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Cursos disponibles",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(courses) { course ->
                    val isCompleted = passedCoursesSet.contains(course.id)
                    CourseListItem(
                        course = course,
                        isCompleted = isCompleted
                    ) {
                        navigateToCourse(userId, course, db, navController)
                    }
                }

                // Botón de cerrar sesión
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val dominicanRed = Color(0xFFD32F2F)
                        Button(
                            onClick = {
                                auth.signOut()
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = dominicanRed,
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = "Cerrar sesión")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Encabezado con gráfico de pastel y nombre de usuario
 */
@Composable
fun HeaderWithPieChart(
    userName: String,
    completedCourses: Int,
    totalCourses: Int
) {
    val fraction = if (totalCourses > 0) completedCourses.toFloat() / totalCourses else 0f
    val percentage = (fraction * 100).roundToInt()

    val dominicanBlue = Color(0xFF1565C0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CourseCompletionPieChart(
            fraction = fraction,
            sizeDp = 120.dp,
            completedColor = dominicanBlue,
            remainingColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$percentage% completado",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Gráfico de pastel
 */
@Composable
fun CourseCompletionPieChart(
    fraction: Float,
    sizeDp: Dp = 120.dp,
    completedColor: Color,
    remainingColor: Color
) {
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = fraction * 360f
            drawArc(
                color = completedColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            drawArc(
                color = remainingColor,
                startAngle = sweepAngle - 90f,
                sweepAngle = 360f - sweepAngle,
                useCenter = true
            )
        }
    }
}

/**
 * Sección con 3 tarjetas (Configuración, Tienda, Medallas)
 */
@Composable
fun DashboardOptionsSection(navController: NavController) {
    val dominicanBlue = Color(0xFF1565C0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Configuración
        OptionCard(
            title = "Configuración",
            iconRes = R.drawable.ic_settings,
            backgroundColor = dominicanBlue,
            contentColor = Color.White
        ) {
            navController.navigate("settings")
        }
        // Tienda
        OptionCard(
            title = "Tienda",
            iconRes = R.drawable.ic_store,
            backgroundColor = dominicanBlue,
            contentColor = Color.White
        ) {
            navController.navigate("store")
        }
        // Medallas
        OptionCard(
            title = "Medallas",
            iconRes = R.drawable.ic_medal,
            backgroundColor = dominicanBlue,
            contentColor = Color.White
        ) {
            navController.navigate("medals")
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OptionCard(
    title: String,
    iconRes: Int,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .size(width = 130.dp, height = 100.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )
        }
    }
}

/**
 * Item de la lista de cursos:
 * Muestra la medalla (si está aprobado y existe URL) o el ícono genérico, escalado y con placeholders.
 */
@Composable
fun CourseListItem(
    course: Course,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(course.title) },
        supportingContent = { Text(course.description) },
        leadingContent = {
            if (isCompleted && course.medalla.isNotEmpty()) {
                // Carga la imagen de la medalla usando Coil con un ImageRequest personalizado
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(course.medalla)
                        .crossfade(true)
                        .size(Size.ORIGINAL) // Podrías usar .size(64,64) para forzar un escalado
                        .placeholder(R.drawable.ic_course)
                        .error(R.drawable.ic_course)
                        .listener(
                            onError = { _: ImageRequest, errorResult ->
                                Log.e("CourseListItem", "Error cargando la medalla", errorResult.throwable)
                            }
                        )
                        .build(),
                    contentDescription = "Medalla del curso",
                    modifier = Modifier
                        .size(32.dp) // Tamaño final en la UI
                        .clip(CircleShape),
                    // Ajusta cómo se escala la imagen dentro del contenedor
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                // Ícono genérico si no está aprobado o no hay URL
                Icon(
                    painter = painterResource(id = R.drawable.ic_course),
                    contentDescription = null
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = null
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
    Divider()
}

/**
 * Lógica para navegar al curso
 */
fun navigateToCourse(
    userId: String?,
    course: Course,
    db: FirebaseFirestore,
    navController: NavController
) {
    if (userId == null) return

    val progressRef = db.collection("progress")
        .document(userId)
        .collection("courses")
        .document(course.id)

    progressRef.get().addOnSuccessListener { document ->
        if (!document.exists()) {
            val progressData = mapOf(
                "courseId" to course.id,
                "progressPercentage" to 0,
                "completed" to false,
                "lastAccessed" to System.currentTimeMillis()
            )
            progressRef.set(progressData).addOnSuccessListener {
                Log.d("MainHomeScreen", "Curso iniciado: ${course.title}")
            }.addOnFailureListener { e ->
                Log.e("MainHomeScreen", "Error al iniciar curso", e)
            }
        }
        navController.navigate("course/${course.id}")
    }.addOnFailureListener { e ->
        Log.e("MainHomeScreen", "Error al verificar progreso del curso", e)
    }
}
