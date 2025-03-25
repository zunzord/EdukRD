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
import com.edukrd.app.navigation.Screen
import com.edukrd.app.viewmodel.ExamViewModel
import com.edukrd.app.viewmodel.ExamState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.edukrd.app.ui.components.DotLoadingIndicator

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
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
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
                            val options = question["answers"] as? List<String> ?: emptyList()

                            Column {
                                Text(questionText, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                options.forEachIndexed { index, answer ->
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
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(onClick = { navController.popBackStack() }) { Text("Volver") }
                        Button(
                            onClick = {
                                val correctCount = exam.questions.count { q ->
                                    val qId = q["questionId"] as? String ?: ""
                                    val correctOption = (q["correctOption"] as? Long)?.toInt() ?: -1
                                    selectedAnswers[qId] == correctOption
                                }
                                val finalScore = (correctCount.toDouble() / exam.questions.size * 100).toInt()
                                val passed = finalScore >= exam.passingScore
                                examViewModel.submitExamResult(courseId, finalScore, passed)
                            },
                            enabled = canSubmit
                        ) { Text("Finalizar") }
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
}
