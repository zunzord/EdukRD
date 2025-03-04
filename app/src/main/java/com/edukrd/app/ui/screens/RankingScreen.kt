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
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    // 1. Especificar tipo explícitamente
    var ranking by remember {
        mutableStateOf<List<Triple<String, String, Int>>>(emptyList())
    }
    var currentUserRank by remember { mutableStateOf<Int?>(null) }
    var currentUserCoins by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dominicanBlue = Color(0xFF1565C0)
    val darkBackground = Color(0xFF0D1B2A)

    LaunchedEffect(Unit) {
        try {
            val usersSnapshot = db.collection("users")
                .orderBy("coins", Query.Direction.DESCENDING)
                .limit(250)
                .get()
                .await()

            val rankedUsers = usersSnapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: "Usuario Anónimo"
                val coins = doc.getLong("coins")?.toInt() ?: 0
                Triple(doc.id, name, coins)
            }

            ranking = rankedUsers

            rankedUsers.forEachIndexed { index, (userId, _, coins) ->
                if (userId == currentUserId) {
                    currentUserRank = index + 1
                    currentUserCoins = coins
                }
            }

            loading = false
        } catch (e: Exception) {
            Log.e("RankingScreen", "Error al obtener ranking", e)
            errorMessage = "Error al cargar el ranking"
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking de Monedas", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Regresar", tint = Color.White)
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
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMessage != null -> Text(
                    errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                ranking.isEmpty() -> Text(
                    "No hay datos de ranking.",
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
                                        "$it",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                    Column {
                                        Text(
                                            // 2. Corregir acceso al nombre
                                            ranking.firstOrNull { user -> user.first == currentUserId }?.second ?: "Tú",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "$currentUserCoins monedas",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            itemsIndexed(ranking) { index, (_, name, coins) ->
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
                                            "${index + 1}",
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
                                                name,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                "$coins monedas",
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