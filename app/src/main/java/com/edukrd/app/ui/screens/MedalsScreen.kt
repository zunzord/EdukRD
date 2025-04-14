package com.edukrd.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.ui.components.DotLoadingIndicator
import com.edukrd.app.ui.components.MedalItem
import com.edukrd.app.ui.components.MedalsProgressBar
import com.edukrd.app.ui.components.MotivationalBubble
import com.edukrd.app.viewmodel.CourseViewModel
import com.edukrd.app.viewmodel.MedalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedalsScreen(navController: NavController) {
    // ViewModel para las medallas
    val medalViewModel: MedalViewModel = hiltViewModel()
    // ViewModel para obtener la lista completa de cursos y los cursos completados.
    val courseViewModel: CourseViewModel = hiltViewModel()

    val medals by medalViewModel.medals.collectAsState()
    val loadingMedals by medalViewModel.loading.collectAsState()
    val errorMedals by medalViewModel.error.collectAsState()

    // Datos necesarios para el progreso global
    val courses by courseViewModel.courses.collectAsState()
    val passedCourseIds by courseViewModel.passedCourseIds.collectAsState()

    // Cargar las medallas y cursos al iniciar.
    LaunchedEffect(Unit) {
        medalViewModel.loadMedals()
        courseViewModel.loadCoursesAndProgress()
    }

    val dominicanBlue = Color(0xFF1565C0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medallero", color = Color.White) },
                colors = topAppBarColors(containerColor = dominicanBlue)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Sección superior: Mensaje explicativo y barra de progreso de medallas.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¡Completa todos los cursos para obtener todas las medallas!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                MedalsProgressBar(
                    courses = courses,
                    passedCourseIds = passedCourseIds,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Sección inferior: listado de medallas
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    loadingMedals -> {
                        DotLoadingIndicator(
                            modifier = Modifier.align(Alignment.Center).size(56.dp)
                        )
                    }
                    errorMedals != null -> {
                        Text(
                            text = errorMedals!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    medals.isEmpty() -> {
                        Text(
                            text = "No has obtenido ninguna medalla.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(medals) { medal ->
                                MedalItem(medal)
                            }
                        }
                    }
                }
            }
        }
    }
    MotivationalBubble(
        message = "GRANDIOSAS! \nson la prueba de tu aprendizaje",
        detailedDescription = "Este es tu medallero. Aquí coleccionas todas las medallas que has obtenido. También, las encontrarás encima del curso al que pertenecen, así sabes que has dominado ese curso. No creas que es su única función; pronto, representarán más de lo que imaginas."
    )
}
