package com.edukrd.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun calculateBottomBarArrowPositions(): Pair<Triple<Float, Float, Float>, Float> {
    // Obtén la configuración actual de la pantalla
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Convertir el ancho y alto de la pantalla a píxeles
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Suponemos que el BottomNavigationBar ocupa 56.dp de alto
    val bottomBarHeightPx = with(density) { 56.dp.toPx() }

    // Calcula la posición Y del centro del BottomNavigationBar
    val barCenterY = (screenHeightPx / 2f) - bottomBarHeightPx

    // Dividir el ancho en 3 secciones para los 3 botones
    val sectionWidth = screenWidthPx / 3f
    val xMedallas = sectionWidth / 2f             // Centro del primer bloque
    val xTienda = sectionWidth + sectionWidth / 2f  // Centro del segundo bloque
    val xRanking = 2 * sectionWidth + sectionWidth / 2f // Centro del tercer bloque

    return Pair(Triple(xMedallas, xTienda, xRanking), barCenterY)
}
