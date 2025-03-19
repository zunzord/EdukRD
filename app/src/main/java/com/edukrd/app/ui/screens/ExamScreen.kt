package com.edukrd.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.viewmodel.ExamViewModel
import com.edukrd.app.viewmodel.ExamState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.ArrowBack


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    navController: NavController,
    courseId: String
) {
    val examViewModel: ExamViewModel = hiltViewModel()
    val examState by examViewModel.examState.collectAsState()
    val loading by examViewModel.loading.collectAsState()
    val error by examViewModel.error.collectAsState()
    val submitResult by examViewModel.submitResult.collectAsState()

    // Para mostrar/ocultar el diálogo de resultados
    var showResultDialog by remember { mutableStateOf(false) }
    var userPassed by remember { mutableStateOf(false) }
    var userScore by remember { mutableStateOf(0) }

    // Mapa para almacenar las respuestas: key = questionId, value = índice de respuesta seleccionada
    val selectedAnswers = remember { mutableStateMapOf<String, Int>() }

    // Cargar los datos del examen al iniciar
    LaunchedEffect(courseId) {
        examViewModel.loadExamData(courseId)
    }

    // Manejar el resultado del envío del examen
    LaunchedEffect(submitResult) {
        submitResult?.let { (success, message) ->
            if (!success) {
                // En caso de error, mostramos un Toast
                Toast.makeText(
                    navController.context,
                    "Error: $message",
                    Toast.LENGTH_LONG
                ).show()
            }
            // Si es success, ya mostramos un AlertDialog, así que no navegamos directamente aquí
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Examen") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                loading -> {
                    CircularProgressIndicator()
                }
                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                examState == null -> {
                    Text(
                        text = "No se encontró examen para este curso.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    val exam: ExamState = examState!!
                    // En vez de mostrar el courseId, ponemos un título genérico
                    Text(
                        text = "Preguntas",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Lista minimalista de preguntas
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(exam.questions) { question ->
                            val questionId = question["questionId"] as? String ?: return@items
                            val questionText = question["question"] as? String ?: "Pregunta sin texto"
                            val options = question["answers"] as? List<String> ?: emptyList()

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = questionText,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                options.forEachIndexed { index, answer ->
                                    val isSelected = selectedAnswers[questionId] == index
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedAnswers[questionId] = index }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = answer)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val isFinishEnabled = (selectedAnswers.size == exam.questions.size)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Volver")
                        }
                        Button(
                            onClick = {
                                // Calcular puntuación
                                val correctCount = exam.questions.count { question ->
                                    val qId = question["questionId"] as? String ?: ""
                                    val correctOption =
                                        (question["correctOption"] as? Long)?.toInt() ?: -1
                                    selectedAnswers[qId] == correctOption
                                }
                                val finalScore = (correctCount.toDouble() / exam.questions.size * 100).toInt()
                                val passed = finalScore >= exam.passingScore

                                // Guardamos estado para mostrar el diálogo
                                userPassed = passed
                                userScore = finalScore
                                showResultDialog = true

                                // Se envía el resultado al repositorio
                                examViewModel.submitExamResult(courseId, finalScore, passed)
                            },
                            enabled = isFinishEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("Finalizar")
                        }
                    }
                }
            }
        }
    }

    // Diálogo de resultado
    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = { /* Evitamos cerrar con click fuera */ },
            // Aquí establecemos el color translúcido azul
            containerColor = Color(0xFF1565C0).copy(alpha = 0.8f),
            textContentColor = Color.White,  // Para que el texto se vea en blanco
            titleContentColor = Color.White,
            title = {
                Text("Resultado del Examen")
            },
            text = {
                // Aplicamos la tipografía Roboto solamente dentro del diálogo
                MaterialTheme(

                ) {
                    if (userPassed) {
                        Text("¡Felicidades! Aprobaste con un $userScore%.")
                    } else {
                        Text("Obtuviste un $userScore%. ¡Inténtalo nuevamente!")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResultDialog = false
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}
