package com.edukrd.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.edukrd.app.models.Course
import com.mackhartley.roundedprogressbar.RoundedProgressBar


@Composable
fun MedalsProgressBar(
    courses: List<Course>,
    passedCourseIds: Set<String>,
    modifier: Modifier = Modifier
) {
    // Estado para mostrar/ocultar la lista de cursos pendientes
    var showPending by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Calcula el total de cursos y cuántos han sido completados
    val totalCourses = courses.size
    val completedCourses = courses.count { passedCourseIds.contains(it.id) }
    val ratio = if (totalCourses > 0) completedCourses.toFloat() / totalCourses else 0f

    // Usar el color primario del tema y su versión con transparencia
    val progressColor = MaterialTheme.colorScheme.primary.toArgb()
    val backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f).toArgb()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { showPending = !showPending },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Progreso de Medallas",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                // Bloque para capturar gestos (aunque en este caso solo se usa el clickable del Card)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            RoundedProgressBar(ctx).apply {
                                val radiusPx = ctx.resources.displayMetrics.density * 28f
                                setCornerRadius(radiusPx, radiusPx, radiusPx, radiusPx)
                                setProgressDrawableColor(progressColor)
                                setBackgroundDrawableColor(backgroundColor)
                                showProgressText(false)
                                setAnimationLength(600L)
                            }
                        },
                        update = { bar ->
                            bar.setProgressPercentage((ratio * 100).toDouble(), true)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(ratio * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    // Puedes agregar íconos adicionales si lo deseas
                }
            }
            // Mostrar la lista de cursos pendientes al pulsar sobre la barra
            if (showPending) {
                Spacer(modifier = Modifier.height(12.dp))
                val pendingCourses = courses.filter { !passedCourseIds.contains(it.id) }
                if (pendingCourses.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Cursos pendientes:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        pendingCourses.forEach { course ->
                            Text(
                                text = course.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable {
                                        // Opcional: al pulsar, por ejemplo, se abre el detalle de ese curso
                                        // o se redirige a alguna parte. Aquí puedes definir el comportamiento.
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(course.imageUrl))
                                        context.startActivity(intent)
                                    }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "¡Felicidades! Has obtenido todas las medallas.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
