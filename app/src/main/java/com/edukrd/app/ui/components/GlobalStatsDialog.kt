package com.edukrd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.ceil

/**
 * GlobalStatsDialog muestra un diálogo modal con:
 * - Encabezado: “Meta global”, total de exámenes aprobados y porcentaje global.
 * - Botones para seleccionar el periodo: Diario, Semanal y Mensual.
 * - Un gráfico lineal que muestra la evolución según el periodo seleccionado.
 *
 * Los datos reales se reciben como parámetros:
 * @param dailyData   -> Lista de Float con exámenes aprobados por día (semana actual).
 * @param weeklyData  -> Lista de Float con exámenes aprobados por semana (mes actual).
 * @param monthlyData -> Lista de Float con exámenes aprobados por mes (año actual).
 */

enum class Period { Daily, Weekly, Monthly }

@Composable
fun GlobalStatsDialog(
    onDismiss: () -> Unit,
    globalProgress: Float,
    totalExamenes: Int,
    dailyData: List<Float>,
    weeklyData: List<Float>,
    monthlyData: List<Float>
) {
    // Definimos los periodos a seleccionar

    var selectedPeriod by remember { mutableStateOf(Period.Daily) }
    val chartData = when (selectedPeriod) {
        Period.Daily -> dailyData
        Period.Weekly -> weeklyData
        Period.Monthly -> monthlyData
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                // Cabecera: "Meta global", total y porcentaje global
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Meta global",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "$totalExamenes exámenes",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Text(
                        text = "${"%.2f".format(globalProgress)}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFF2E7D32), // Verde
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Ícono de estrella en la esquina superior derecha (sin acción por ahora)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Estrella",
                        tint = Color.Yellow,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp)
                            .size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones para seleccionar el periodo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PeriodButton(
                        label = "Diario",
                        selected = (selectedPeriod == Period.Daily),
                        onClick = { selectedPeriod = Period.Daily }
                    )
                    PeriodButton(
                        label = "Semanal",
                        selected = (selectedPeriod == Period.Weekly),
                        onClick = { selectedPeriod = Period.Weekly }
                    )
                    PeriodButton(
                        label = "Mensual",
                        selected = (selectedPeriod == Period.Monthly),
                        onClick = { selectedPeriod = Period.Monthly }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gráfico lineal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE3F2FD))
                ) {
                    LineChart(
                        data = chartData,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        lineColor = Color(0xFF673AB7)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón para cerrar
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

/**
 * PeriodButton: Botón minimalista para alternar entre periodos.
 */
@Composable
private fun PeriodButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else Color.Black
    Text(
        text = label,
        color = contentColor,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

/**
 * LineChart: Dibuja un gráfico de línea simple usando Canvas.
 */
@Composable
fun LineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.Blue,
    strokeWidth: Float = 4f
) {
    if (data.isEmpty()) return

    Canvas(modifier = modifier) {
        val minValue = data.minOrNull() ?: 0f
        val maxValue = data.maxOrNull() ?: 0f
        val range = maxValue - minValue

        // Distancia horizontal entre cada punto
        val spacing = size.width / (data.size - 1).coerceAtLeast(1)

        val path = Path()

        // Función para mapear el valor 'y' a coordenadas (invertido para que el mayor quede arriba)
        fun getYPos(value: Float): Float {
            val ratio = (value - minValue) / (range.coerceAtLeast(0.0001f))
            return size.height - (ratio * size.height)
        }

        path.moveTo(0f, getYPos(data[0]))
        for (i in 1 until data.size) {
            val x = spacing * i
            val y = getYPos(data[i])
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Dibujar círculos en cada punto
        for (i in data.indices) {
            val x = spacing * i
            val y = getYPos(data[i])
            drawCircle(
                color = lineColor,
                radius = strokeWidth * 1.2f,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}
