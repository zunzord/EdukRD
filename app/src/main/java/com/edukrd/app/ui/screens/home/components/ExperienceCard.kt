package com.edukrd.app.ui.screens.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edukrd.app.R
import com.edukrd.app.models.Course
import com.edukrd.app.ui.components.AsyncImageWithShimmer

@Composable
fun ExperienceCard(
    course: Course,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(200.dp)
            .height(240.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val coverUrl = course.imageUrl
            if (coverUrl.isNotBlank()) {
                AsyncImageWithShimmer(
                    url = coverUrl,
                    contentDescription = course.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_course),
                    contentDescription = "Imagen genérica",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isCompleted && course.medalla.isNotBlank()) {
                AsyncImageWithShimmer(
                    url = course.medalla,
                    contentDescription = "Medalla",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
        }
    }
}
