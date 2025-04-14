package com.edukrd.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.navigation.Screen
import com.edukrd.app.ui.components.DotLoadingIndicator
import com.edukrd.app.viewmodel.ExamState
import com.edukrd.app.viewmodel.ExamViewModel
import kotlinx.coroutines.launch

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
    // Mapa para almacenar las respuestas seleccionadas, usando la questionId
    val selectedAnswers = remember { mutableStateMapOf<String, Int>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(courseId) {
        examViewModel.loadExamData(courseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Examen") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                loading -> DotLoadingIndicator(modifier = Modifier.size(56.dp))
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                examState == null -> Text("No se encontró examen para este curso.", color = MaterialTheme.colorScheme.error)
                else -> {
                    val exam: ExamState = examState!!
                    Text("Preguntas", style = MaterialTheme.typography.titleLarge)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(exam.questions) { question ->
                            val questionId = question["questionId"] as? String ?: return@items
                            val questionText = question["question"] as? String ?: ""
                            val randomizedOptions = question["randomizedOptions"] as? List<String> ?: emptyList()

                            Column {
                                Text(
                                    text = questionText,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                randomizedOptions.forEachIndexed { index, answer ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = selectedAnswers[questionId] == index,
                                            onClick = { selectedAnswers[questionId] = index }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(answer)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    val canSubmit = selectedAnswers.size == exam.questions.size

                    Button(
                        onClick = {
                            scope.launch {
                                // Calcular respuestas correctas y el porcentaje final
                                val correctCount = exam.questions.count { q ->
                                    val qId = q["questionId"] as? String ?: ""
                                    val newCorrectOption = q["newCorrectOption"] as? Int ?: -1
                                    selectedAnswers[qId] == newCorrectOption
                                }
                                val totalQuestions = exam.questions.size
                                val finalScore = (correctCount.toDouble() / totalQuestions * 100).toInt()
                                val passed = finalScore >= exam.passingScore

                                // Calcular las monedas ganadas usando el método suspendido
                                val coinsEarned = if (passed) examViewModel.getCoinsEarned(courseId) else 0

                                // Registrar el resultado en backend
                                examViewModel.submitExamResult(courseId, finalScore, passed)

                                // Navegar a la nueva pantalla de resultados pasando todos los datos necesarios.
                                navController.navigate(
                                    Screen.ExamResultScreen.createRoute(
                                        courseId = courseId,
                                        finalScore = finalScore,
                                        correctCount = correctCount,
                                        totalQuestions = totalQuestions,
                                        passed = passed,
                                        coinsEarned = coinsEarned
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canSubmit) MaterialTheme.colorScheme.primary else Color.White,
                            contentColor = if (canSubmit) Color.White else Color.Black
                        )
                    ) {
                        Text("Finalizar")
                    }
                }
            }
        }
    }
}





/*package com.edukrd.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.navigation.Screen
import com.edukrd.app.ui.components.DotLoadingIndicator
import com.edukrd.app.ui.components.AsyncImageWithShimmer
import com.edukrd.app.viewmodel.ExamViewModel
import com.edukrd.app.viewmodel.ExamState
import kotlinx.coroutines.launch

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

    // Mapa para almacenar las respuestas seleccionadas, usando la questionId
    val selectedAnswers = remember { mutableStateMapOf<String, Int>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(courseId) {
        examViewModel.loadExamData(courseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Examen") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                /*actions = {
                    IconButton(onClick = { /* Por ahora sin funciones */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                }*/
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                loading -> DotLoadingIndicator(modifier = Modifier.size(56.dp))
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                examState == null -> Text("No se encontró examen para este curso.", color = MaterialTheme.colorScheme.error)
                else -> {
                    val exam: ExamState = examState!!
                    Text("Preguntas", style = MaterialTheme.typography.titleLarge)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(exam.questions) { question ->
                            // Extraemos los datos necesarios
                            val questionId = question["questionId"] as? String ?: return@items
                            val questionText = question["question"] as? String ?: ""
                            // respuestas randomizadas
                            val randomizedOptions = question["randomizedOptions"] as? List<String> ?: emptyList()

                            Column {
                                Text(
                                    text = questionText,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                randomizedOptions.forEachIndexed { index, answer ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = selectedAnswers[questionId] == index,
                                            onClick = { selectedAnswers[questionId] = index }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(answer)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    // valida si se pueden enviar todas las respuestas.
                    val canSubmit = selectedAnswers.size == exam.questions.size



                    Button(
                        onClick = {

                            val correctCount = exam.questions.count { q ->
                                val qId = q["questionId"] as? String ?: ""
                                val newCorrectOption = q["newCorrectOption"] as? Int ?: -1
                                selectedAnswers[qId] == newCorrectOption
                            }
                            val finalScore = (correctCount.toDouble() / exam.questions.size * 100).toInt()
                            val passed = finalScore >= exam.passingScore
                            examViewModel.submitExamResult(courseId, finalScore, passed)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canSubmit) MaterialTheme.colorScheme.primary else Color.White,
                            contentColor = if (canSubmit) Color.White else Color.Black
                        )
                    ) {
                        Text("Finalizar")
                    }
                }
            }
        }
    }

    submitResult?.let { (success, message) ->
        AlertDialog(
            onDismissRequest = { examViewModel.resetSubmitResult() },
            title = { Text(if (success) "¡Aprobado!" else "Resultado") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = {
                    examViewModel.resetSubmitResult()
                    if (success) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }) {
                    Text("Aceptar")
                }
            },
            containerColor = if (success) Color(0xFF4CAF50) else Color(0xFFF44336),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}*/
