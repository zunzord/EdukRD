package com.edukrd.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.ui.components.DotLoadingIndicator
import com.edukrd.app.ui.components.MotivationalBubble
import com.edukrd.app.viewmodel.RankingViewModel
import com.google.firebase.auth.FirebaseAuth


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(navController: NavController) {
    val rankingViewModel: RankingViewModel = hiltViewModel()
    val ranking by rankingViewModel.ranking.collectAsState()
    val currentUserRank by rankingViewModel.currentUserRank.collectAsState()
    val currentUserCoins by rankingViewModel.currentUserCoins.collectAsState()
    val loading by rankingViewModel.loading.collectAsState()
    val error by rankingViewModel.error.collectAsState()

    val dominicanBlue = Color(0xFF1565C0)
    val darkBackground = Color(0xFF0D1B2A)
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        rankingViewModel.loadRanking()
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking de Monedas", color = Color.White) },
               /* navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White
                        )
                    }
                },*/
                colors = topAppBarColors(containerColor = dominicanBlue)
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
                loading -> {
                    DotLoadingIndicator(modifier = Modifier.align(Alignment.Center).size(56.dp))
                }
                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                ranking.isEmpty() -> {
                    Text(
                        text = "No hay datos de ranking.",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }
                else -> {
                    Column {
                        currentUserRank?.let { rank ->
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
                                        text = "$rank",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                    Column {
                                        // Buscamos el nombre del usuario actual en la lista (si existe)
                                        val currentName = ranking.firstOrNull { it.first == currentUserId }?.second ?: "Tú"
                                        Text(
                                            text = currentName,
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$currentUserCoins monedas",
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
                            itemsIndexed(ranking) { index, entry ->
                                val (userId, name, coins) = entry
                                val medalIcon = when (index) {
                                    0, 1, 2, 3 -> Icons.Filled.EmojiEvents
                                    else -> Icons.Filled.Star
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.1f)
                                    )
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
                                                text = "$coins monedas",
                                                fontSize = 14.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }

                                    }
                                }

                                HorizontalDivider()

                            }
                        }
                    }
                }
            }
        }
    }
    MotivationalBubble(
        message = "Se el # 1! \nAhorra y sube, o canjea y caerás.",
        detailedDescription = "La premisa es sencilla: Quieres ser el número 1? solo debes ahorrar tus monedas, ya que estas son la forma de escalar. Mientras mas monedas, las posiciones subes en el ranking...pero espera, seguro que no te interesa nada de la tienda? ve con cuidado, al canjear puedes perder posiciones; recuerda, tus monedas son tu escalera en el ranking."
    )
}
