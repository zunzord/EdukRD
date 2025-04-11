    package com.edukrd.app.ui.screens

    import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.edukrd.app.ui.components.DotLoadingIndicator
import com.edukrd.app.ui.components.MotivationalBubble
import com.edukrd.app.viewmodel.MedalData
import com.edukrd.app.viewmodel.MedalViewModel

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MedalsScreen(navController: NavController) {
        val medalViewModel: MedalViewModel = hiltViewModel()
        val medals by medalViewModel.medals.collectAsState()
        val loading by medalViewModel.loading.collectAsState()
        val error by medalViewModel.error.collectAsState()

        // Cargar las medallas desde el ViewModel (UID se obtiene internamente)
        LaunchedEffect(Unit) {
            medalViewModel.loadMedals()
        }




        val dominicanBlue = Color(0xFF1565C0)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Medallero", color = Color.White) },
                    /*navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
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
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
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
                    medals.isEmpty() -> {
                        Text(
                            text = "No has obtenido ninguna medalla.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        //carrucel horizontal
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
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
        MotivationalBubble(
            message = "GRANDIOSAS! \nson la prueba de tu aprendizaje",
            detailedDescription = "Este es tu medallero. Aquí coleccionas todas las medallas que has obtenido. También, las encontraras encima del curso al que pertenecen. Así, sabes que has dominado ese curso. No creas que es su única función; pronto, representarán más de lo que imaginas."
        )
    }



    /**
     * Muestra una medalla en tamaño grande. Al pulsar, aparece/oculta un overlay
     * semitransparente con el nombre de la medalla.
     */
    @Composable
    fun MedalItem(medal: MedalData) {
        // Controla la visibilidad del overlay con el título
        var showTitle by remember { mutableStateOf(false) }
        val sizeMedal = 280.dp

        Box(
            modifier = Modifier
                .size(sizeMedal)
                .clip(CircleShape) // Recorta en forma circular
                .clickable { showTitle = !showTitle },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = medal.imageUrl,
                contentDescription = medal.title,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp), // Agrega un padding interno, para que la imagen no se "pegue" al borde.
                contentScale = ContentScale.Fit  // Usa Fit para que la imagen se ajuste sin recortes excesivos.
            )

            // Si se toca la medalla, se muestra un overlay con el título
            if (showTitle) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = medal.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
