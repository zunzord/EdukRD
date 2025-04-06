package com.edukrd.app.ui.screens.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import com.edukrd.app.R
import kotlin.math.max
import kotlin.math.min

/**
 * Modelo para cada paso del tutorial.
 *
 * @param arrowX Porcentaje en [0f..1f] de la posición horizontal de la flecha sobre la imagen.
 * @param arrowY Porcentaje en [0f..1f] de la posición vertical de la flecha sobre la imagen.
 * @param bubbleAlignment Dónde se alineará la burbuja (TOP, BOTTOM, etc.).
 */
data class TutorialItem(
    val id: Int,
    val title: String,
    val description: String,
    val arrowX: Float,     // Porcentaje horizontal [0..1]
    val arrowY: Float,     // Porcentaje vertical [0..1]
    val arrowIcon: Int,    // Recurso de flecha (ic_up_arrow, ic_down_arrow, etc.)
    val bubbleAlignment: Alignment = Alignment.TopCenter
)

/**
 * Lista de elementos a describir:
 *  1) Configuración
 *  2) Logout
 *  3) Barra de objetivos
 *  4) Medallas
 *  5) Tienda
 *  6) Ranking
 *
 */
val defaultTutorialItems = listOf(
    TutorialItem(
        id = 1,
        title = "Configuración",
        description = "Aquí puedes modificar tus preferencias y ajustes de la app.",
        arrowX = 0.065f,  // Ajusta
        arrowY = 0.14f,  // Ajusta
        arrowIcon = R.drawable.ic_up_arrow, // Por ejemplo, flecha apuntando hacia abajo
        bubbleAlignment = Alignment.Center
    ),
    TutorialItem(
        id = 2,
        title = "Cerrar sesión",
        description = "Presiona aquí para salir de la aplicación.",
        arrowX = 0.90f, // Ajusta
        arrowY = 0.14f, // Ajusta
        arrowIcon = R.drawable.ic_up_arrow,
        bubbleAlignment = Alignment.Center
    ),
    TutorialItem(
        id = 3,
        title = "Barra de objetivos",
        description = "Consulta tu objetivo diario y, al pulsar, verás el semanal y mensual.",
        arrowX = 0.50f,
        arrowY = 0.56f,
        arrowIcon = R.drawable.ic_down_arrow,
        bubbleAlignment = Alignment.TopEnd
    ),
    TutorialItem(
        id = 4,
        title = "Medallas",
        description = "Visualiza las medallas que has obtenido en tus cursos.",
        arrowX = 0.38f,
        arrowY = 0.85f,
        arrowIcon = R.drawable.ic_down_arrow, // Flecha apuntando hacia arriba
        bubbleAlignment = Alignment.Center
    ),
    TutorialItem(
        id = 5,
        title = "Tienda",
        description = "Canjea tus monedas y ve tus artículos disponibles.",
        arrowX = 0.60f,
        arrowY = 0.85f,
        arrowIcon = R.drawable.ic_down_arrow,
        bubbleAlignment = Alignment.Center
    ),
    TutorialItem(
        id = 6,
        title = "Ranking",
        description = "Revisa tu posición en el ranking de usuarios.",
        arrowX = 0.87f,
        arrowY = 0.85f,
        arrowIcon = R.drawable.ic_down_arrow,
        bubbleAlignment = Alignment.Center
    )
)

/**
 * Composable que muestra el tutorial con:
 *  - Una imagen de fondo (la captura de la HomeScreen)
 *  - Flecha para el elemento actual
 *  - Burbuja con título, descripción, botones de Anterior/Siguiente
 */
@Composable
fun TutorialOverlay(
    modifier: Modifier = Modifier,
    tutorialItems: List<TutorialItem> = defaultTutorialItems,
    onDismiss: () -> Unit
) {
    // Manejo del índice del paso actual
    var currentIndex by remember { mutableStateOf(0) }
    val currentItem = tutorialItems.getOrNull(currentIndex) ?: return

    // Obtenemos las dimensiones del contenedor (aquí usamos LocalConfiguration)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Para que la imagen de fondo ocupe toda la pantalla
    val imageModifier = Modifier.fillMaxSize()

    // Tamaño fijo para la flecha
    val arrowSize = 40.dp

    // Calculamos la posición de la flecha usando los porcentajes del TutorialItem
    // Multiplicamos por las dimensiones de la pantalla (o del contenedor del tutorial)
    var arrowPosX = (currentItem.arrowX.coerceIn(0f, 1f) * screenWidth.value).dp
    var arrowPosY = (currentItem.arrowY.coerceIn(0f, 1f) * screenHeight.value).dp

    // Definimos zonas seguras para los elementos superiores e inferiores
    val topSafeZone = 54.dp + 8.dp   // Por ejemplo, altura de la barra superior + margen
    val bottomSafeZone = 56.dp + 8.dp // Por ejemplo, altura de la barra inferior + margen

    // Ajustamos la posición vertical en función de la flecha:
    if (currentItem.arrowIcon == R.drawable.ic_down_arrow) {
        // Flecha apuntando hacia abajo: se coloca en una zona que no se superponga a la parte superior
        arrowPosY = arrowPosY.coerceAtLeast(topSafeZone)
    } else if (currentItem.arrowIcon == R.drawable.ic_up_arrow) {
        // Flecha apuntando hacia arriba: se coloca en una zona que no se superponga a la parte inferior
        arrowPosY = arrowPosY.coerceAtMost(screenHeight - bottomSafeZone)
    }

    // Calculamos la posición final de la flecha (centrándola respecto al icono)
    val finalArrowX = arrowPosX - arrowSize / 2
    val finalArrowY = arrowPosY - arrowSize / 2

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        // Imagen de fondo (screenshot) que ocupa toda la pantalla
        Image(
            painter = painterResource(id = R.drawable.tutorialphoto),
            contentDescription = "Tutorial background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight)
        )

        // Flecha
        Icon(
            painter = painterResource(id = currentItem.arrowIcon),
            contentDescription = "Tutorial Arrow",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(arrowSize * 1.3f)
                .absoluteOffset(x = finalArrowX, y = finalArrowY)
                .zIndex(2f)
        )

        // Burbuja con información del paso
        // Se posiciona según el bubbleAlignment definido en cada TutorialItem
        val bubbleModifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(16.dp)
            .align(currentItem.bubbleAlignment)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            modifier = bubbleModifier
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentItem.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentItem.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentIndex > 0) {
                        Button(
                            onClick = { currentIndex-- },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Anterior")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Button(
                        onClick = {
                            if (currentIndex < tutorialItems.lastIndex) {
                                currentIndex++
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        val label = if (currentIndex < tutorialItems.lastIndex) "Siguiente" else "Ok"
                        Text(label)
                    }
                }
            }
        }
    }
}







/*@Composable
fun TutorialOverlay(
    tutorialSteps: List<TutorialStep>,
    onDismiss: () -> Unit
) {
    var currentStepIndex by remember { mutableStateOf(0) }
    val currentStep = tutorialSteps[currentStepIndex]

    // Obtenemos el tamaño de la pantalla en dp
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val arrowSize = 48.dp

    // Función que devuelve la posición base según el alignment
    fun basePositionForAlignment(alignment: Alignment): Pair<Dp, Dp> {
        return when (alignment) {
            Alignment.TopCenter -> Pair(screenWidth / 2 - arrowSize / 2, 0.dp)
            Alignment.TopStart -> Pair(0.dp, 0.dp)
            Alignment.TopEnd -> Pair(screenWidth - arrowSize, 0.dp)
            Alignment.CenterStart -> Pair(0.dp, screenHeight / 2 - arrowSize / 2)
            Alignment.Center -> Pair(screenWidth / 2 - arrowSize / 2, screenHeight / 2 - arrowSize / 2)
            Alignment.CenterEnd -> Pair(screenWidth - arrowSize, screenHeight / 2 - arrowSize / 2)
            Alignment.BottomStart -> Pair(0.dp, screenHeight - arrowSize)
            Alignment.BottomCenter -> Pair(screenWidth / 2 - arrowSize / 2, screenHeight - arrowSize)
            Alignment.BottomEnd -> Pair(screenWidth - arrowSize, screenHeight - arrowSize)
            else -> Pair(screenWidth / 2 - arrowSize / 2, 0.dp)
        }
    }

    // Calculamos la posición base de la flecha a partir del alignment
    val (baseX, baseY) = currentStep.arrowAlignment?.let { basePositionForAlignment(it) } ?: Pair(0.dp, 0.dp)
    // Sumamos el offset adicional (convertido a Dp) y "clampeamos" para que la flecha se mantenga dentro de la pantalla
    val finalArrowX = (/*baseX +*/ currentStep.arrowOffsetX.dp).coerceIn(0.dp, screenWidth - arrowSize)
    val finalArrowY = (/*baseY +*/ currentStep.arrowOffsetY.dp).coerceIn(0.dp, screenHeight - arrowSize)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        // Dibujamos la flecha usando la posición calculada
        if (currentStep.arrowAlignment != null) {
            val arrowIcon = when (currentStep.arrowAlignment) {
                Alignment.TopCenter -> R.drawable.ic_back_arrow//R.drawable.ic_down_arrow
                Alignment.BottomCenter -> R.drawable.ic_up_arrow
                Alignment.CenterStart -> R.drawable.ic_foward_arrow
                Alignment.CenterEnd -> R.drawable.ic_back_arrow
                else -> R.drawable.ic_down_arrow
            }
            Icon(
                painter = painterResource(id = arrowIcon),
                contentDescription = "Flecha del tutorial",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(arrowSize)
                    .absoluteOffset(x = finalArrowX, y = finalArrowY)
                    .zIndex(1f)
            )
        }

        // La burbuja se posiciona de acuerdo al targetAlignment y se le aplica el offset adicional
        val bubbleModifier = Modifier
            .align(currentStep.targetAlignment)
            .absoluteOffset(x = currentStep.bubbleOffsetX.dp, y = currentStep.bubbleOffsetY.dp)
            .padding(16.dp)
            .fillMaxWidth(0.9f)

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            modifier = bubbleModifier
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentStep.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                    color = Color.Black
                )
                Text(
                    text = currentStep.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    if (currentStepIndex > 0) {
                        Button(
                            onClick = { currentStepIndex-- },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Anterior")
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            if (currentStepIndex < tutorialSteps.lastIndex) {
                                currentStepIndex++
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = if (currentStepIndex < tutorialSteps.lastIndex) "Siguiente" else "Entendido")
                    }
                }
            }
        }
    }
}*/
