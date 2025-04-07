package com.edukrd.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.edukrd.app.viewmodel.ReportViewModel

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    reportViewModel: ReportViewModel = hiltViewModel()
) {
    // Estados para los campos que ingresa el usuario
    var type by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observamos el estado de resultado del envío del reporte
    val submissionResult by reportViewModel.submitResult.collectAsState()

    // Si se envió el reporte exitosamente, se cierra el diálogo
    LaunchedEffect(submissionResult) {
        submissionResult?.let { result ->
            // Si el primer valor del par es true, el envío fue exitoso.
            if (result.first) {
                Toast.makeText(context, "¡Tu reporte ha sido enviado correctamente!", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            // Se puede mostrar un mensaje (Toast, Snackbar, etc.) aquí si se desea.
            // Luego se resetea el estado para evitar repetir el efecto.
            reportViewModel.resetSubmitResult()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Enviar Reporte",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SimpleDropdownMenu(
                    options = listOf("Sugerencia", "Incidencia"),
                    selectedOption = type,
                    onOptionSelected = { type = it },
                    label = "Indica el tipo de reporte"
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Mensaje") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.LightGray)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            reportViewModel.submitReport(type, message)
                        },
                        enabled = type.isNotBlank() && message.isNotBlank()
                    ) {
                        // plan de usar indicador de progreso, agregando un estado loading en el ViewModel.
                        if (submissionResult?.first == null && submissionResult == null) {

                            Text("Enviar")
                        } else {

                            DotLoadingIndicator(

                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
