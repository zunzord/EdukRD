package com.edukrd.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable


@Composable
fun ActiveSessionDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Sesión activa detectada") },
        text = { Text("Ya hay una sesión activa en otro dispositivo. ¿Deseas cerrar la sesión anterior y continuar?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Sí, cerrar sesión anterior")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text("Cancelar")
            }
        }
    )
}
