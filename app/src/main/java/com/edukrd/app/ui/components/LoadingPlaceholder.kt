package com.edukrd.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.edukrd.app.R

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
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop  
) {
    val painter = rememberAsyncImagePainter(url)
    val loading = painter.state is AsyncImagePainter.State.Loading

    Box(modifier) {
        if (loading) {
            ShimmerBox(Modifier.matchParentSize())
        }
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale
        )
    }
}

@Composable
fun DotLoadingIndicator(
    modifier: Modifier = Modifier,
    dotSize: Dp = 16.dp
) {

    val composition = rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.loading_animation)).value


    if (composition != null) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = modifier.size(dotSize)
        )
    }
}
