package com.edukrd.app.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.size

@Composable
fun RatingPopup(
    courseTitle: String,
    onDismiss: () -> Unit,
    onSubmit: (stars: Int, feedback: String) -> Unit
) {
    var localStars by remember { mutableStateOf(0) }
    var feedbackText by remember { mutableStateOf("") }
    // Habilitar el botón de "OK" si se selecciona al menos una estrella
    val isEnabled = localStars > 0
    // Límite de caracteres para el feedback
    val maxChars = 200

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = courseTitle,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Fila de estrellas para seleccionar la puntuación
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        androidx.compose.material3.IconButton(
                            onClick = { localStars = index + 1 }
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = if (index < localStars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Star ${index + 1}",
                                tint = if (index < localStars) Color(0xFFFFD700) else Color.Gray,
                                modifier = Modifier.size(40.dp) // Aumenta el tamaño para mayor visibilidad
                            )
                        }
                    }
                }

                // Campo de feedback siempre visible, con límite de caracteres
                if (localStars > 0) {
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = {
                            if (it.length <= maxChars) {
                                feedbackText = it
                            }
                        },
                        label = { Text("Feedback") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                // Botón de confirmación
                OutlinedButton(
                    onClick = {
                        val sanitizedFeedback = feedbackText.trim()
                        onSubmit(localStars, sanitizedFeedback)
                        onDismiss()
                    },
                    enabled = isEnabled,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
