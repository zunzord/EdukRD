package com.edukrd.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edukrd.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultScreen(
    courseId: String,
    finalScore: Int,
    correctCount: Int,
    totalQuestions: Int,
    passed: Boolean,
    coinsEarned: Int,
    onContinue: () -> Unit,
    onRetry: () -> Unit
) {
    BackHandler {}//evita retroceso

    Scaffold(
        topBar = {
            TopAppBar(title = {})
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ExamResultFeedback(
                finalScore = finalScore,
                correctCount = correctCount,
                totalQuestions = totalQuestions,
                passed = passed,
                coinsEarned = coinsEarned
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (passed) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "Continuar", color = MaterialTheme.colorScheme.onPrimary)
                }
            } else {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = "Reintentar", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

/**
 * Componente reusable para mostrar el feedback motivacional en la pantalla de resultados.
 *
 * Muestra:
 * - Una imagen (de congratulación, de máximo diario alcanzado o de "inténtalo de nuevo" según el resultado).
 * - Un mensaje principal.
 * - Estadísticas del examen.
 */
@Composable
fun ExamResultFeedback(
    finalScore: Int,
    correctCount: Int,
    totalQuestions: Int,
    passed: Boolean,
    coinsEarned: Int
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        when {
            passed && coinsEarned > 0 -> {
                Image(
                    painter = painterResource(id = R.drawable.congratulations),
                    contentDescription = "¡Felicidades!",
                    modifier = Modifier
                        .fillMaxWidth(), // Toma todo el ancho
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.TopStart

                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Has aprobado el examen y has ganado $coinsEarned monedas.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            passed && coinsEarned == 0 -> {
                Image(
                    painter = painterResource(id = R.drawable.max_daily_reached),  // Imagen específica para máximo diario alcanzado
                    contentDescription = "Máximo diario alcanzado",
                    modifier = Modifier
                        .fillMaxWidth(), // Toma todo el ancho
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.TopStart

                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Has aprobado el examen, pero ya alcanzaste el máximo diario de monedas.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = R.drawable.try_again),
                    contentDescription = "Inténtalo de nuevo",
                    modifier = Modifier
                        .fillMaxWidth(), // Toma todo el ancho
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.TopStart

                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No alcanzaste la puntuación mínima.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Puntuación: $finalScore%",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Respuestas correctas: $correctCount / $totalQuestions",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
