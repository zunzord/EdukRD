package com.edukrd.app.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.edukrd.app.R

/**
 * Modelo para cada paso del tutorial.
 * targetAlignment y arrowAlignment son valores predefinidos para ubicar la burbuja y la flecha (fallback).
 * Los campos arrowOffsetX/Y y bubbleOffsetX/Y permiten ajustar la posición en píxeles.
 */
data class TutorialStep(
    val id: Int,
    val title: String,
    val description: String,
    val targetAlignment: Alignment,
    val arrowAlignment: Alignment? = null,
    val arrowOffsetX: Int = 0,
    val arrowOffsetY: Int = 0,
    val bubbleOffsetX: Int = 0,
    val bubbleOffsetY: Int = 0
)

/**
 * TutorialOverlay muestra un overlay secuencial que guía al usuario paso a paso.
 * Se calcula la posición final de la flecha en función del alignment base y el offset adicional,
 * y se limita para que no se salga de la pantalla.
 */
@Composable
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
}
