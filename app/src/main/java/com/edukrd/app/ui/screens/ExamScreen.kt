package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.edukrd.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    navController: NavController,
    userId: String,
    courseId: String
) {
    val db = FirebaseFirestore.getInstance()

    var questions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedAnswers by remember { mutableStateOf(mutableMapOf<String, Int>()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passingScore by remember { mutableStateOf(0) }
    var totalQuestions by remember { mutableStateOf(0) }

    // Para mostrar el resultado final
    var showResultDialog by remember { mutableStateOf(false) }
    var userPassed by remember { mutableStateOf(false) }
    var userScore by remember { mutableStateOf(0) }
    var awardedCoins by remember { mutableStateOf(0) }

    // Corutina para llamar a funciones suspend
    val scope = rememberCoroutineScope()

    // Cargar el examen
    LaunchedEffect(courseId) {
        try {
            val examSnapshot = db.collection("exams")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()

            if (!examSnapshot.isEmpty) {
                val examDoc = examSnapshot.documents[0]
                passingScore = (examDoc.getLong("passingScore") ?: 0).toInt()
                totalQuestions = (examDoc.getLong("totalQuestions") ?: 0).toInt()

                val questionsSnapshot = examDoc.reference
                    .collection("questions")
                    .orderBy("order")
                    .get()
                    .await()

                questions = questionsSnapshot.documents.mapNotNull { it.data }
            } else {
                errorMessage = "No se encontró un examen para este curso."
            }
        } catch (e: Exception) {
            Log.e("ExamScreen", "Error al obtener el examen", e)
            errorMessage = "Error al cargar el examen."
        }
        loading = false
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
    } else {
        // Mostrar las preguntas
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(text = "Examen", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(questions) { question ->
                val questionId = question["questionId"] as? String ?: return@items
                val options = question["answers"] as? List<String> ?: emptyList()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = question["question"] as? String ?: "Pregunta sin texto",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Opciones
                        options.forEachIndexed { index, answer ->
                            val isSelected = selectedAnswers[questionId] == index
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedAnswers = selectedAnswers.toMutableMap().apply {
                                            this[questionId] = index
                                        }
                                    }
                                )
                                Text(text = answer, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }

            // Botones al final
            item {
                Spacer(modifier = Modifier.height(16.dp))

                val dominicanBlue = Color(0xFF1565C0)
                val dominicanRed = Color(0xFFD32F2F)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dominicanBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Volver")
                    }

                    val isFinishEnabled = (selectedAnswers.size == totalQuestions)
                    Button(
                        onClick = {
                            // Llamar a la corutina para procesar el examen
                            scope.launch {
                                submitExamWithCoins(
                                    userId = userId,
                                    courseId = courseId,
                                    selectedAnswers = selectedAnswers,
                                    questions = questions,
                                    passingScore = passingScore,
                                    db = db
                                ) { finalScore, passed, coinsEarned ->
                                    userScore = finalScore
                                    userPassed = passed
                                    awardedCoins = coinsEarned
                                    showResultDialog = true
                                }
                            }
                        },
                        enabled = isFinishEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dominicanRed,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Finalizar")
                    }
                }
            }
        }

        if (showResultDialog) {
            AlertDialog(
                onDismissRequest = { /* Evitar cerrar si tocan fuera */ },
                title = { Text("Resultados del Examen") },
                text = {
                    if (userPassed) {
                        Text("¡Felicidades! Aprobaste con $userScore%. Has ganado $awardedCoins monedas.")
                    } else {
                        Text("Obtuviste un $userScore%. ¡Inténtalo nuevamente!")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResultDialog = false
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Ir a Inicio")
                    }
                }
            )
        }
    }
}

/**
 * Función suspend que evalúa el examen y otorga monedas según la lógica:
 * - Primera vez que aprueba un curso: +10 coins
 * - Veces posteriores (mismo curso) en el mismo día: +1 coin
 * - Límite de 5 exámenes por día (en total). Si ya se hicieron 5, no otorga monedas.
 */
suspend fun submitExamWithCoins(
    userId: String,
    courseId: String,
    selectedAnswers: Map<String, Int>,
    questions: List<Map<String, Any>>,
    passingScore: Int,
    db: FirebaseFirestore,
    onComplete: (finalScore: Int, passed: Boolean, coinsEarned: Int) -> Unit
) {
    // 1. Calcular score
    var score = 0
    for (question in questions) {
        val questionId = question["questionId"] as? String ?: continue
        val correctOption = (question["correctOption"] as? Long)?.toInt() ?: -1
        if (selectedAnswers[questionId] == correctOption) {
            score++
        }
    }
    val finalScore = (score.toDouble() / questions.size * 100).toInt()
    val passed = finalScore >= passingScore

    // 2. Crear registro del examen en examResults
    val examResultData = mapOf(
        "userId" to userId,
        "courseId" to courseId,
        "score" to finalScore,
        "passed" to passed,
        "date" to Timestamp.now()
    )

    // 3. Verificar la cantidad de exámenes que el usuario ha hecho hoy (cualquier curso)
    val now = Timestamp.now()
    val (startOfDay, endOfDay) = getDayRange(now)  // Función para obtener rango de hoy

    // Query: examResults del user con date entre startOfDay y endOfDay
    val dailySnapshot = db.collection("examResults")
        .whereEqualTo("userId", userId)
        .whereGreaterThanOrEqualTo("date", startOfDay)
        .whereLessThan("date", endOfDay)
        .get()
        .await()
    val dailyAttempts = dailySnapshot.size()

    // 4. Si ya hizo 5 exámenes hoy, no otorgar monedas
    var coinsEarned = 0
    if (dailyAttempts < 5 && passed) {
        // 5. Verificar si es la primera vez que aprueba este curso
        val firstTimeSnapshot = db.collection("examResults")
            .whereEqualTo("userId", userId)
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("passed", true)
            .limit(1)
            .get()
            .await()

        val isFirstTime = firstTimeSnapshot.isEmpty
        coinsEarned = if (isFirstTime) 10 else 1
    }

    // 6. Guardar el examen y actualizar las monedas
    try {
        // Guardar el resultado
        val docRef = db.collection("examResults").document()
        docRef.set(examResultData).await()

        // Si coinsEarned > 0, incrementar monedas
        if (coinsEarned > 0) {
            val userRef = db.collection("users").document(userId)
            userRef.update("coins", com.google.firebase.firestore.FieldValue.increment(coinsEarned.toLong()))
                .await()
        }

        onComplete(finalScore, passed, coinsEarned)
    } catch (e: Exception) {
        Log.e("ExamScreen", "Error al guardar examen o actualizar monedas", e)
        onComplete(finalScore, passed, 0)
    }
}

/**
 * Retorna el rango [startOfDay, endOfDay) para la fecha de 'timestamp'.
 */
fun getDayRange(timestamp: Timestamp): Pair<Timestamp, Timestamp> {
    // Convertir a milisegundos
    val millis = timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000
    val date = java.util.Date(millis)

    val calendar = java.util.Calendar.getInstance()
    calendar.time = date
    // Ajustar a inicio del día
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    val startMillis = calendar.timeInMillis

    // Fin del día
    calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
    val endMillis = calendar.timeInMillis

    val startTimestamp = Timestamp(startMillis / 1000, ((startMillis % 1000) * 1000000).toInt())
    val endTimestamp = Timestamp(endMillis / 1000, ((endMillis % 1000) * 1000000).toInt())
    return Pair(startTimestamp, endTimestamp)
}
