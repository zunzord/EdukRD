package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class MedalData(
    val courseId: String,
    val title: String,
    val imageUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedalsScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    var medals by remember { mutableStateOf<List<MedalData>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val dominicanBlue = Color(0xFF1565C0)

    LaunchedEffect(userId) {
        if (userId == null) {
            errorMessage = "Usuario no autenticado."
            loading = false
            return@LaunchedEffect
        }

        try {
            val examResultsSnapshot = db.collection("examResults")
                .whereEqualTo("userId", userId)
                .whereEqualTo("passed", true)
                .get()
                .await()

            val passedCourseIds = examResultsSnapshot.documents.mapNotNull {
                it.getString("courseId")
            }.toSet()

            if (passedCourseIds.isEmpty()) {
                loading = false
                return@LaunchedEffect
            }

            val tempMedals = mutableListOf<MedalData>()
            for (courseId in passedCourseIds) {
                val courseDoc = db.collection("courses")
                    .document(courseId)
                    .get()
                    .await()

                if (courseDoc.exists()) {
                    val title = courseDoc.getString("title") ?: "Curso sin título"
                    val medallaUrl = courseDoc.getString("medalla") ?: ""
                    if (medallaUrl.isNotEmpty()) {
                        tempMedals.add(
                            MedalData(
                                courseId = courseId,
                                title = title,
                                imageUrl = medallaUrl
                            )
                        )
                    }
                }
            }

            medals = tempMedals
            loading = false
        } catch (e: Exception) {
            Log.e("MedalsScreen", "Error al obtener medallas", e)
            errorMessage = "Error al obtener medallas: ${e.message}"
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medallero", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = dominicanBlue,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                medals.isEmpty() -> {
                    Text(
                        text = "No has obtenido ninguna medalla.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(medals) { medal ->
                            MedalItem(medal = medal)
                        }
                    }
                }
            }

            // Botones inferiores
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate("home") {
                            popUpTo("medals") { inclusive = true }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    },
                    text = { Text("Volver", color = Color.White) },
                    containerColor = dominicanBlue
                )

                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("ranking") },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Leaderboard,
                            contentDescription = "Ranking",
                            tint = Color.White
                        )
                    },
                    text = { Text("Ranking", color = Color.White) },
                    containerColor = dominicanBlue
                )
            }
        }
    }
}

@Composable
fun MedalItem(medal: MedalData) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = medal.imageUrl,
                contentDescription = "Medalla del curso",
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = medal.title,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}