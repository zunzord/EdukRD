package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.edukrd.app.R
import com.edukrd.app.models.Course
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    var userName by remember { mutableStateOf("Usuario") }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Para el gráfico de pastel
    var completedCourses by remember { mutableStateOf(0) }
    var totalCourses by remember { mutableStateOf(0) }

    // Carga de datos
    LaunchedEffect(userId) {
        if (userId == null) {
            errorMessage = "Usuario no autenticado."
            loading = false
            return@LaunchedEffect
        }

        try {
            // 1. Datos de usuario
            val userSnapshot = db.collection("users").document(userId).get().await()
            if (userSnapshot.exists()) {
                userName = userSnapshot.getString("name") ?: "Usuario"
            } else {
                errorMessage = "No se encontraron datos del usuario."
            }

            // 2. Cursos totales
            val coursesSnapshot = db.collection("courses").get().await()
            val coursesList = coursesSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Course::class.java)?.copy(id = doc.id)
            }
            courses = coursesList
            totalCourses = coursesList.size

            // 3. Cursos aprobados en examResults (estructura plana)
            val passedDocs = db.collection("examResults")
                .whereEqualTo("userId", userId)
                .whereEqualTo("passed", true)
                .get().await()
            // Se cuentan los cursos únicos aprobados
            val passedCourses = passedDocs.documents.mapNotNull { it.getString("courseId") }.toSet()
            completedCourses = passedCourses.size

            loading = false
        } catch (e: Exception) {
            Log.e("MainHomeScreen", "Error al obtener datos", e)
            errorMessage = "Error al obtener datos."
            loading = false
        }
    }

    // Interfaz principal
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
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
                // Sección de encabezado: gráfico de pastel
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
                    CourseListItem(
                        course = course,
                        onClick = {
                            navigateToCourse(userId, course, db, navController)
                        }
                    )
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
                        Button(onClick = {
                            auth.signOut()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CourseCompletionPieChart(
            fraction = fraction,
            sizeDp = 120.dp,
            completedColor = MaterialTheme.colorScheme.primary,
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
            // Porción completada
            drawArc(
                color = completedColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            // Porción restante
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
 * Sección con 3 tarjetas (Configuración, Medallas, Tienda)
 */
@Composable
fun DashboardOptionsSection(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Configuración
        OptionCard(
            title = "Configuración",
            iconRes = R.drawable.ic_settings
        ) {
            navController.navigate("settings")
        }
        // Medallas
        OptionCard(
            title = "Medallas",
            iconRes = R.drawable.ic_medal
        ) {
            navController.navigate("medals")
        }
        // Tienda
        OptionCard(
            title = "Tienda",
            iconRes = R.drawable.ic_store
        ) {
            navController.navigate("store")
        }
    }
}

/**
 * Card individual para cada opción
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionCard(
    title: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .size(width = 100.dp, height = 100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Lista de cursos en formato ListItem + HorizontalDivider
 */
@Composable
fun CourseListItem(
    course: Course,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(course.title) },
        supportingContent = { Text(course.description) },
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.ic_course),
                contentDescription = null
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = null
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
    // Reemplazamos HorizontalDivider() por Divider() de Material 3
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
