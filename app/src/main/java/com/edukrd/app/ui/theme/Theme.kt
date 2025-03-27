package com.edukrd.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.edukrd.app.R

// Colores principales
val DominicanRed = Color(0xFFD32F2F)
val DominicanBlue = Color(0xFF1565C0)
val White = Color(0xFFFFFFFF)
val DarkGray = Color(0xFF121212)
val LightGray = Color(0xFFF5F5F5)

// Paleta de colores para modo claro
private val LightColorScheme = lightColorScheme(
    primary = DominicanBlue,
    secondary = DominicanRed,
    background = White,
    surface = LightGray,
    onPrimary = White,
    onSecondary = White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

// Paleta de colores para modo oscuro
private val DarkColorScheme = darkColorScheme(
    primary = DominicanBlue,
    secondary = DominicanRed,
    background = DarkGray,
    surface = Color.Black,
    onPrimary = White,
    onSecondary = White,
    onBackground = White,
    onSurface = White
)

// 1) Definimos la familia tipográfica local:
val MontserratFamily = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold)
    // Agrega más si las tienes (extra_light, italic, etc.)
)

// 2) Creamos un objeto Typography con Montserrat
val EdukRDTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp

    ),
    headlineMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )
    // Ajusta y añade más estilos según lo necesites
)

// 3) Definimos el Theme con nuestras paletas de colores y la tipografía local
@Composable
fun EdukRDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = EdukRDTypography,
        content = content
    )
}
