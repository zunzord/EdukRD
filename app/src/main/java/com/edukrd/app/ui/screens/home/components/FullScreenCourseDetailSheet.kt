package com.edukrd.app.ui.screens.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults as M3ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edukrd.app.R
import com.edukrd.app.models.Course
import com.edukrd.app.ui.components.AsyncImageWithShimmer
import com.edukrd.data.viewmodel.RatingViewModel
import com.edukrd.app.ui.screens.home.components.RatingPopup
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults as M3ButtonDefaults2
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import kotlin.math.roundToInt
import androidx.compose.runtime.collectAsState

@Composable
fun FullScreenCourseDetailSheet(
    course: Course,
    isCompleted: Boolean,
    coinReward: Int,
    onClose: () -> Unit,
    onTakeCourse: () -> Unit
) {
    val ratingVm: RatingViewModel = hiltViewModel()
    val avgRating by ratingVm.avgRating.collectAsState()
    val userRating by ratingVm.userRating.collectAsState()
    var showRatingDialog by remember { mutableStateOf(false) }

    // Cargar ratings para el curso
    LaunchedEffect(course.id) {
        ratingVm.loadRatings(course.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Sección superior: imagen de curso y botón de volver
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            val coverUrl = course.imageUrl
            if (coverUrl.isNotBlank()) {
                AsyncImageWithShimmer(
                    url = coverUrl,
                    contentDescription = course.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_course),
                    contentDescription = "Imagen genérica",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Contenido del detalle del curso
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$coinReward monedas",
                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Sección de rating: al pulsar se muestra el popup para puntuar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRatingDialog = true }
                    .padding(vertical = 8.dp)
            ) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (index < avgRating.roundToInt()) Color(0xFFFFD700) else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%.1f", avgRating),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (course.description.isNotBlank()) {
                Text(
                    text = course.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onTakeCourse,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Iniciar Curso", color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    // Popup para puntuar el curso
    if (showRatingDialog) {
        RatingPopup(
            courseTitle = course.title,
            onDismiss = { showRatingDialog = false },
            onSubmit = { stars, feedback ->
                ratingVm.submitRating(course.id, stars, feedback)
                showRatingDialog = false
            }
        )
    }
}
