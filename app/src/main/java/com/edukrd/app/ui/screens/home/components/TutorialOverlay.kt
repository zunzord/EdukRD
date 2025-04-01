package com.edukrd.app.ui.screens.home.components

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
 * Si se asignan offsets (arrowOffsetX/Y y bubbleOffsetX/Y) en el TutorialStep,
 * se utilizan para posicionar la flecha y la burbuja de forma precisa.
 */
@Composable
fun TutorialOverlay(
    tutorialSteps: List<TutorialStep>,
    onDismiss: () -> Unit
) {
    var currentStepIndex by remember { mutableStateOf(0) }
    val currentStep = tutorialSteps[currentStepIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        // Mostrar la flecha si se definió arrowAlignment.
        currentStep.arrowAlignment?.let { arrowAlign ->
            // Selecciona el ícono adecuado según el Alignment.
            val arrowIcon = when (arrowAlign) {
                Alignment.TopCenter -> R.drawable.ic_down_arrow
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
                    .size(48.dp)
                    .offset(x = currentStep.arrowOffsetX.dp, y = currentStep.arrowOffsetY.dp)
                    .align(arrowAlign)
            )
        }

        // Configuramos el modificador de la burbuja con offset para evitar superposición con la flecha.
        val bubbleModifier = Modifier
            .align(currentStep.targetAlignment)
            .offset(x = currentStep.bubbleOffsetX.dp, y = currentStep.bubbleOffsetY.dp)
            .padding(16.dp)
            .fillMaxWidth(0.9f)

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            modifier = bubbleModifier
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
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
                    horizontalArrangement = Arrangement.SpaceBetween
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
