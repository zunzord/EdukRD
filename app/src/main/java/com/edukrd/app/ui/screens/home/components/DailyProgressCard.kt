package com.edukrd.app.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.edukrd.app.R
import com.mackhartley.roundedprogressbar.RoundedProgressBar


@Composable
fun DailyProgressBar(
    dailyCurrent: Int,
    dailyTarget: Int,
    onDailyTargetClick: () -> Unit,
    onBannerIconPosition: ((Offset) -> Unit)? = null

) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onDailyTargetClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Meta diaria",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val ratio = if (dailyTarget > 0) dailyCurrent.toFloat() / dailyTarget else 0f
            val progressColor = MaterialTheme.colorScheme.primary.toArgb()
            val backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f).toArgb()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = if (onBannerIconPosition != null)
                                Modifier.onGloballyPositioned { coordinates ->
                                    onBannerIconPosition(coordinates.positionInRoot())
                                } else Modifier
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.target),
                                contentDescription = "Target Icon",
                                modifier = Modifier.size(24.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Text(
                            text = dailyCurrent.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
