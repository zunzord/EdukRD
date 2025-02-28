package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Importaciones adicionales necesarias para delegados:
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

@Composable
fun ExamScreen(
    navController: NavController,
    userId: String,
    courseId: String
) {
    val db = FirebaseFirestore.getInstance()

    // Recuerda que para usar "by remember { mutableStateOf(...) }"
    // necesitas las importaciones de getValue y setValue
    var questions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedAnswers by remember { mutableStateOf(mutableMapOf<String, Int>()) }

    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Datos del examen
    // Sustituimos mutableIntStateOf(0) por mutableStateOf(0) para mayor compatibilidad
    var passingScore by remember { mutableStateOf(0) }
    var totalQuestions by remember { mutableStateOf(0) }

    // Efecto para cargar el examen
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

                Log.d(
                    "ExamScreen",
                    "Cargadas ${questions.size} preguntas. totalQuestions: $totalQuestions, passingScore: $passingScore"
                )
            } else {
                errorMessage = "No se encontró un examen para este curso."
            }
        } catch (e: Exception) {
            Log.e("ExamScreen", "Error al obtener el examen", e)
            errorMessage = "Error al cargar el examen."
        }
        loading = false
    }

    // Interfaz
    if (loading) {
        // Indicador de carga
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (errorMessage != null) {
        // Mensaje de error
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
    } else {
        // Mostrar preguntas y botón "Finalizar"
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Título
            item {
                Text(
                    text = "Examen",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Lista de preguntas
            items(questions) { question ->
                val questionId = question["questionId"] as? String
                if (questionId == null) {
                    Log.e("ExamScreen", "Pregunta sin 'questionId', se omitirá.")
                    return@items
                }

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

                        // Opciones de respuesta
                        options.forEachIndexed { index, answer ->
                            val isSelected = selectedAnswers[questionId] == index
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        // Actualizamos la respuesta seleccionada
                                        selectedAnswers = selectedAnswers.toMutableMap().apply {
                                            this[questionId] = index
                                        }
                                        Log.d(
                                            "ExamScreen",
                                            "selectedAnswers[$questionId] = $index. " +
                                                    "Ahora hay ${selectedAnswers.size} respuestas seleccionadas."
                                        )
                                    }
                                )
                                Text(
                                    text = answer,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Botones al final
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Botón "Volver"
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Volver")
                    }

                    // Botón "Finalizar"
                    val isFinishEnabled = (selectedAnswers.size == totalQuestions)
                    Log.d(
                        "ExamScreen",
                        "Botón Finalizar habilitado? $isFinishEnabled | " +
                                "selectedAnswers.size=${selectedAnswers.size}, " +
                                "totalQuestions=$totalQuestions"
                    )
                    Button(
                        onClick = {
                            submitExam(
                                userId = userId,
                                courseId = courseId,
                                selectedAnswers = selectedAnswers,
                                questions = questions,
                                passingScore = passingScore,
                                db = db
                            ) {
                                // Regresar tras guardar
                                navController.popBackStack()
                            }
                        },
                        enabled = isFinishEnabled
                    ) {
                        Text("Finalizar")
                    }
                }
            }
        }
    }
}

// Función para evaluar el examen y guardar resultados
fun submitExam(
    userId: String,
    courseId: String,
    selectedAnswers: Map<String, Int>,
    questions: List<Map<String, Any>>,
    passingScore: Int,
    db: FirebaseFirestore,
    onComplete: () -> Unit
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

    // Nuevo formato: cada resultado se guarda en la colección "examResults"
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
            onComplete()
        }
        .addOnFailureListener { e ->
            Log.e("ExamScreen", "Error al guardar el examen", e)
        }
}
