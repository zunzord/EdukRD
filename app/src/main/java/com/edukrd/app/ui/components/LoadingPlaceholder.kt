package com.edukrd.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

@Composable
private fun shimmerBrush(translate: Float): Brush {
    val colors = listOf(
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
    )
    return Brush.linearGradient(colors, Offset(translate, translate), Offset(translate + 200f, translate + 200f))
}

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart)
    )
    Box(modifier.background(shimmerBrush(translate), RoundedCornerShape(8.dp)))
}

@Composable
fun LoadingPlaceholder() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Box(Modifier.fillMaxWidth().height(200.dp)) { ShimmerBox(Modifier.fillMaxSize()) }
        Spacer(Modifier.height(24.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(3) {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.size(200.dp, 240.dp)) {
                    ShimmerBox(Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun AsyncImageWithShimmer(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val painter = rememberAsyncImagePainter(url)
    val loading = painter.state is AsyncImagePainter.State.Loading

    Box(modifier) {
        if (loading) ShimmerBox(Modifier.matchParentSize())
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun DotLoadingIndicator(
    modifier: Modifier = Modifier,
    dotSize: Dp = 16.dp
) {
    val transition = rememberInfiniteTransition()
    val delays = listOf(0, 150, 300)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        delays.forEachIndexed { index, delay ->
            val scale by transition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    tween(800, delayMillis = delay, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                )
            )
            val alpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(800, delayMillis = delay, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                )
            )
            Box(
                Modifier
                    .width(dotSize)
                    .height(dotSize)
                    .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .background(
                        color = if (index % 2 == 0) Color(0xFF00008B) else Color(0xFF8B0000),
                        shape = CircleShape
                    )
            )
        }
    }
}
