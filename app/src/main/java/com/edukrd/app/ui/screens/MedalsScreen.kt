    package com.edukrd.app.ui.screens

    import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
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



        // Puedes reutilizar tu color principal, por ejemplo:
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
                        // Mostramos las medallas en un carrusel horizontal
                        LazyRow(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
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
            detailedDescription = "Este es tu medallero. Aquí colecciones todas las medallas que hsa obenido. Además, las medallas se muestran encima del curso al que pertenecen. Así, sabes que has dominado ese curso. No creas que es su unica función; en el futuro, representarán mas de lo que imaginas."
        )
    }



    /**
     * Muestra una medalla en tamaño grande. Al pulsar, aparece/oculta un overlay
     * semitransparente con el nombre de la medalla.
     */
    @Composable
    fun MedalItem(medal: MedalData) {
        // Controla la visibilidad del overlay
        var showTitle by remember { mutableStateOf(false) }

        // Ajusta el tamaño según tu preferencia
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RectangleShape)
                .clickable { showTitle = !showTitle }
        ) {
            // Imagen principal de la medalla
            AsyncImage(
                model = medal.imageUrl,
                contentDescription = medal.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Si se pulsó la medalla, muestra overlay con el título
            if (showTitle) {
                // Fondo semitransparente para resaltar el texto
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            // Por ejemplo, un overlay negro con alpha
                            Color.Black.copy(alpha = 0.4f)
                                // compositeOver(...) te permite ajustar la mezcla si deseas
                                .compositeOver(Color.Transparent)
                        )
                ) {
                    Text(
                        text = medal.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

    }
