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
import kotlinx.coroutines.tasks.await
import com.edukrd.app.R

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
    var showResultDialog by remember { mutableStateOf(false) }
    var userPassed by remember { mutableStateOf(false) }
    var userScore by remember { mutableStateOf(0) }

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
                            submitExam(
                                userId = userId,
                                courseId = courseId,
                                selectedAnswers = selectedAnswers,
                                questions = questions,
                                passingScore = passingScore,
                                db = db
                            ) { finalScore, passed ->
                                userScore = finalScore
                                userPassed = passed
                                showResultDialog = true
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
                onDismissRequest = { /* No cerrar al tocar fuera */ },
                title = { Text("Resultados del Examen") },
                text = {
                    if (userPassed) {
                        Text("¡Felicidades! Aprobaste con $userScore%. Medalla obtenida.")
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


fun submitExam(
    userId: String,
    courseId: String,
    selectedAnswers: Map<String, Int>,
    questions: List<Map<String, Any>>,
    passingScore: Int,
    db: FirebaseFirestore,
    onComplete: (finalScore: Int, passed: Boolean) -> Unit
) {
    var score = 0
    questions.forEach { question ->
        val questionId = question["questionId"] as? String ?: return@forEach
        val correctOption = (question["correctOption"] as? Long)?.toInt() ?: -1
        if (selectedAnswers[questionId] == correctOption) {
            score++
        }
    }

    val finalScore = (score.toDouble() / questions.size * 100).toInt()
    val passed = finalScore >= passingScore

    val result = mapOf(
        "userId" to userId,
        "courseId" to courseId,
        "score" to finalScore,
        "passed" to passed,
        "date" to Timestamp.now()
    )

    db.collection("examResults")
        .add(result)
        .addOnSuccessListener {
            Log.d("ExamScreen", "Examen guardado exitosamente: $result")
            onComplete(finalScore, passed)
        }
        .addOnFailureListener { e ->
            Log.e("ExamScreen", "Error al guardar el examen", e)
            onComplete(finalScore, passed)
        }
}
