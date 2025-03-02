package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.text.font.FontWeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    var ranking by remember { mutableStateOf<List<Triple<String, String, Int>>>(emptyList()) }
    var currentUserRank by remember { mutableStateOf<Int?>(null) }
    var currentUserName by remember { mutableStateOf<String>("Usuario") }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val examResultsSnapshot = db.collection("examResults")
                .whereEqualTo("passed", true)
                .get()
                .await()

            val userMedalsCount = mutableMapOf<String, MutableSet<String>>()

            for (document in examResultsSnapshot.documents) {
                val userId = document.getString("userId")
                val courseId = document.getString("courseId")

                if (userId != null && courseId != null) {
                    userMedalsCount.getOrPut(userId) { mutableSetOf() }.add(courseId)
                }
            }

            val sortedRanking = userMedalsCount.map { (userId, courses) ->
                userId to courses.size
            }.sortedByDescending { it.second }
                .take(250)

            val userList = mutableListOf<Triple<String, String, Int>>()
            for ((index, entry) in sortedRanking.withIndex()) {
                val (userId, medals) = entry
                val userSnapshot = db.collection("users").document(userId).get().await()
                val userName = userSnapshot.getString("name") ?: "Usuario Desconocido"

                userList.add(Triple(userId, userName, medals))

                if (userId == currentUserId) {
                    currentUserRank = index + 1
                    currentUserName = userName
                }
            }

            ranking = userList
            loading = false
        } catch (e: Exception) {
            Log.e("RankingScreen", "Error al obtener el ranking", e)
            errorMessage = "Error al cargar el ranking"
            loading = false
        }
    }

    val dominicanBlue = Color(0xFF1565C0)
    val darkBackground = Color(0xFF0D1B2A)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking de Medallas", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = dominicanBlue)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBackground)
                .padding(innerPadding)
        ) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                errorMessage != null -> Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                ranking.isEmpty() -> Text(
                    text = "No hay datos de ranking.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
                else -> {
                    Column {

                        currentUserRank?.let {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(containerColor = dominicanBlue)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$currentUserRank",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = currentUserName,
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tu posición en el ranking",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        // 🔹 Lista de ranking
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            itemsIndexed(ranking) { index, (_, name, medals) ->
                                val medalIcon = when (index) {
                                    0 -> Icons.Filled.EmojiEvents
                                    1 -> Icons.Filled.EmojiEvents
                                    2 -> Icons.Filled.EmojiEvents
                                    3 -> Icons.Filled.EmojiEvents
                                    else -> Icons.Filled.Star
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                        Icon(
                                            imageVector = medalIcon,
                                            contentDescription = "Medalla",
                                            modifier = Modifier.size(32.dp),
                                            tint = when (index) {
                                                0 -> Color(0xFFFFD700)
                                                1 -> Color(0xFFC0C0C0)
                                                2 -> Color(0xFFCD7F32)
                                                3 -> Color(0xFF8A8A8A)
                                                else -> Color(0xFFFFD700)
                                            }
                                        )
                                        Column(
                                            modifier = Modifier.padding(start = 16.dp)
                                        ) {
                                            Text(
                                                text = name,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "$medals medallas",
                                                fontSize = 14.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
