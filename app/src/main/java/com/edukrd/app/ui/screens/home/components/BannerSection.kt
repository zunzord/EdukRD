package com.edukrd.app.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edukrd.app.R
import com.edukrd.app.models.UserGoalsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset

@Composable
fun BannerSection(
    bannerUrl: String,
    userName: String,
    userGoalsState: UserGoalsState,
    onDailyTargetClick: () -> Unit,
    onBannerIconPosition: ((Offset) -> Unit)? = null
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            // Puedes utilizar bannerUrl si tienes imágenes dinámicas; en este ejemplo se usa un recurso fijo.
            Image(
                painter = painterResource(id = R.drawable.fondo),
                contentDescription = "Banner de la pantalla Home",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            Text(
                text = "Hola, $userName!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }
        // Invoca el componente para mostrar el progreso diario (DailyProgressBar)
        DailyProgressBar(
            dailyCurrent = userGoalsState.dailyCurrent,
            dailyTarget = userGoalsState.dailyTarget,
            onDailyTargetClick = onDailyTargetClick,
            onBannerIconPosition = onBannerIconPosition
        )
    }
}
