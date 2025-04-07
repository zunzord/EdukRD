package com.edukrd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MotivationalBubble(
    message: String,
    detailedDescription: String,
    durationMillis: Long = 10000L //milisegundos
) {
    var showBubble by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // temporalizador para ocultar
    LaunchedEffect(Unit) {
        delay(durationMillis)
        showBubble = false
    }

    // Contenedor para detectar toques en cualquier parte y ocultar la burbuja
    if (showBubble) {
        Box(modifier = Modifier
            .fillMaxSize()
            .clickable { showBubble = false } // Tocar fuera de la burbuja la oculta
        ) {
            // Burbuja
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .clickable { showDialog = true } // diálogo
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(30.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message,
                    color = Color.DarkGray,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }

    // Diálogo con información detallada
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Sabías que") },
            text = { Text(text = detailedDescription) },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
