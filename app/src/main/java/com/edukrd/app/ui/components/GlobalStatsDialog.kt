package com.edukrd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.DrawStyle
import com.edukrd.app.R
import androidx.compose.material.icons.filled.ArrowBack

enum class Period { Daily, Weekly, Monthly }

@Composable
fun GlobalStatsDialog(
    onDismiss: () -> Unit,
    dailyCurrent: Int,
    dailyTarget: Int,
    weeklyCurrent: Int,
    weeklyTarget: Int,
    monthlyCurrent: Int,
    monthlyTarget: Int,
    dailyData: List<Float>,
    weeklyData: List<Float>,
    monthlyData: List<Float>
) {
    var selectedPeriod by remember { mutableStateOf(Period.Daily) }

    // Aseguramos que las listas tengan al menos dos datos para graficar
    val safeDailyData = if (dailyData.size < 2) dailyData + listOf(0f) else dailyData
    val safeWeeklyData = if (weeklyData.size < 2) weeklyData + listOf(0f) else weeklyData
    val safeMonthlyData = if (monthlyData.size < 2) monthlyData + listOf(0f) else monthlyData

    // Usamos los datos según el periodo seleccionado (sin alterar la lógica del UseCase)
    val chartData = when (selectedPeriod) {
        Period.Daily -> safeDailyData
        Period.Weekly -> safeWeeklyData
        Period.Monthly -> safeMonthlyData
    }

    // --- MODIFICACIÓN: Calcular el porcentaje usando los valores pasados sin sobrecalculo ---
    // Nota: Estos cálculos se hacen "directamente" a partir de los datos del UseCase.
    val percentage = when (selectedPeriod) {
        Period.Daily -> if (dailyTarget > 0) dailyCurrent.toFloat() / dailyTarget * 100f else 0f
        Period.Weekly -> if (weeklyTarget > 0) weeklyCurrent.toFloat() / weeklyTarget * 100f else 0f
        Period.Monthly -> if (monthlyTarget > 0) monthlyCurrent.toFloat() / monthlyTarget * 100f else 0f
    }
    // --- FIN MODIFICACIÓN ---

    // Etiquetas para el eje X según el periodo
    val xAxisLabels = when (selectedPeriod) {
        Period.Daily -> listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        Period.Weekly -> listOf("1", "2", "3", "4")
        Period.Monthly -> listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Botón de retorno para cerrar el diálogo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Cabecera: muestra el porcentaje calculado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${"%.2f".format(percentage)}%",
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    // Muestra la estrella si se alcanza o supera el 100%
                    if (percentage >= 100f) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.star),
                            contentDescription = "Meta alcanzada",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones para seleccionar el periodo (sin cambios en la lógica)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PeriodButton(
                            label = "Diario",
                            selected = (selectedPeriod == Period.Daily)
                        ) { selectedPeriod = Period.Daily }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PeriodButton(
                            label = "Semanal",
                            selected = (selectedPeriod == Period.Weekly)
                        ) { selectedPeriod = Period.Weekly }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PeriodButton(
                            label = "Mensual",
                            selected = (selectedPeriod == Period.Monthly)
                        ) { selectedPeriod = Period.Monthly }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contenedor del gráfico
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE3F2FD))
                ) {
                    val line = Line(
                        label = when (selectedPeriod) {
                            Period.Daily -> "Diario"
                            Period.Weekly -> "Semanal"
                            Period.Monthly -> "Mensual"
                        },
                        values = chartData.map { it.toDouble() },
                        color = SolidColor(MaterialTheme.colorScheme.primary),
                        drawStyle = DrawStyle.Stroke(width = 3.dp)
                    )
                    LineChart(
                        data = listOf(line),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp)
                    )
                }

                // Etiquetas del eje X
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    xAxisLabels.forEach { label ->
                        Box(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PeriodButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else Color.Black

    Text(
        text = label,
        color = contentColor,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        textAlign = TextAlign.Center
    )
}
