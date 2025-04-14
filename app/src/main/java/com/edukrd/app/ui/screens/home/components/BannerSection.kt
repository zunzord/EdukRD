package com.edukrd.app.ui.screens.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.edukrd.app.R
import com.edukrd.app.models.UserGoalsState
import com.edukrd.app.viewmodel.BannerViewModel

@Composable
fun BannerSection(
    userName: String,
    userGoalsState: UserGoalsState,
    onDailyTargetClick: () -> Unit,
    onBannerIconPosition: ((Offset) -> Unit)? = null,
    viewModel: BannerViewModel = hiltViewModel()
) {
    // Observamos la lista de URLs y el índice actual del carrusel desde el ViewModel
    val imageUrls by viewModel.imageUrls.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                // Detector de gestos personalizado para distinguir entre swipe y long-press
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            // Espera el primer toque en la pantalla.
                            awaitFirstDown(requireUnconsumed = false)
                            // Pausamos el auto-scroll tan pronto se detecta el toque.
                            viewModel.pauseAutoScroll()
                            var totalDragX = 0f
                            var dragConsumed = false

                            // Mientras el dedo permanezca en pantalla, se monitorean los eventos.
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue

                                // Calcula el delta a partir de la diferencia entre la posición actual y la anterior
                                val dragDelta = change.position - change.previousPosition
                                totalDragX += dragDelta.x

                                // Solo si el desplazamiento acumulado supera 50f se actualiza el índice
                                if (!dragConsumed && kotlin.math.abs(totalDragX) > 50f) {
                                    if (totalDragX > 50f) {
                                        viewModel.updateIndex(-1)
                                    } else if (totalDragX < -50f) {
                                        viewModel.updateIndex(1)
                                    }
                                    dragConsumed = true
                                }
                                // Consumimos todo el evento para evitar actualizaciones múltiples en un solo gesto.
                                change.consume()
                            } while (event.changes.any { it.pressed })

                            // Reanuda el auto-scroll al levantarse todos los dedos.
                            viewModel.resumeAutoScroll()
                        }
                    }
                }
        ) {
            // Se usa Crossfade para lograr una transición orgánica al cambiar la imagen.
            Crossfade(targetState = currentIndex) { index ->
                // Si la lista está vacía, se utiliza una imagen por defecto.
                val bannerImage = if (imageUrls.isNotEmpty()) {
                    imageUrls.getOrNull(index) ?: ""
                } else {
                    ""
                }
                AsyncImage(
                    model = bannerImage,
                    contentDescription = "Banner de la pantalla Home",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.fondo),
                    error = painterResource(id = R.drawable.fondo)
                )
            }
            // Overlay de degradado para mejorar la visibilidad del texto
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            Text(
                text = "Hola, $userName!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }

        // Se invoca el componente para mostrar el progreso diario
       /* DailyProgressBar(
            dailyCurrent = userGoalsState.dailyCurrent,
            dailyTarget = userGoalsState.dailyTarget,
            onDailyTargetClick = onDailyTargetClick,
            onBannerIconPosition = onBannerIconPosition
        )*/
    }
}
