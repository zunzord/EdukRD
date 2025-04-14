package com.edukrd.app.ui.components


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.edukrd.app.viewmodel.MedalData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


@Composable
fun MedalItem(medal: MedalData) {
    var showTitle by remember { mutableStateOf(false) }
    val sizeMedal = 280.dp

    Box(
        modifier = Modifier
            .size(sizeMedal)
            .clip(CircleShape) // Recorta en forma circular
            .clickable { showTitle = !showTitle },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = medal.imageUrl,
            contentDescription = medal.title,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp), // Espacio interno para que la imagen no se pegue al borde.
            contentScale = ContentScale.Fit
        )
        if (showTitle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = medal.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}
