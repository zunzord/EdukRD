package com.edukrd.app.ui.screens.home.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edukrd.app.R
import com.edukrd.app.models.Course
import com.edukrd.app.ui.components.AsyncImageWithShimmer
import com.edukrd.data.viewmodel.RatingViewModel
import kotlin.math.roundToInt

@Composable
fun FullScreenCourseDetailSheet(
    course: Course,
    isCompleted: Boolean,
    coinReward: Int,
    onClose: () -> Unit,
    onTakeCourse: () -> Unit
) {
    // ViewModel del rating y su estado
    val ratingVm: RatingViewModel = hiltViewModel()
    val avgRating by ratingVm.avgRating.collectAsState()
    val userRating by ratingVm.userRating.collectAsState()
    var showRatingDialog by remember { mutableStateOf(false) }

    // Estado para controlar la visualización de las referencias (dialogo emergente)
    var showReferencesDialog by remember { mutableStateOf(false) }

    // Cargar ratings para el curso
    LaunchedEffect(course.id) {
        ratingVm.loadRatings(course.id)
    }

    val context = LocalContext.current

    // Contenedor principal
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Contenido principal desplazable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Sección superior: imagen del curso y botón de volver
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
            // Detalles del curso
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
                // Sección de rating
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
                // Descripción del curso
                if (course.description.isNotBlank()) {
                    Text(
                        text = course.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                // Botón "Referencias" debajo de la descripción
                if (course.referencias.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { showReferencesDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Referencias",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Botón para iniciar el curso
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
        // Diálogo emergente para mostrar las referencias
        if (showReferencesDialog) {
            AlertDialog(
                onDismissRequest = { showReferencesDialog = false },
                title = { Text(text = "Referencias") },
                text = {
                    Column {
                        course.referencias.forEach { ref ->
                            Text(
                                text = ref,
                                modifier = Modifier
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ref))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showReferencesDialog = false }) {
                        Text(text = "Cerrar")
                    }
                }
            )
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





/*package com.edukrd.app.ui.screens.home.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edukrd.app.R
import com.edukrd.app.models.Course
import com.edukrd.app.ui.components.AsyncImageWithShimmer
import com.edukrd.data.viewmodel.RatingViewModel
import kotlin.math.roundToInt

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

    // Variable para controlar la visibilidad de las referencias en la parte inferior izquierda
    var showReferences by remember { mutableStateOf(false) }

    // Cargar ratings para el curso
    LaunchedEffect(course.id) {
        ratingVm.loadRatings(course.id)
    }

    val context = LocalContext.current

    // Contenedor principal que permite superponer la sección de referencias fija
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Contenido principal desplazable
        Column(
            modifier = Modifier
                .fillMaxSize()
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
            // Contenido de detalle del curso
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
                // Sección de rating
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
        // Sección de referencias fija en la esquina inferior izquierda
        if (course.referencias.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = if (showReferences) "Ocultar referencias" else "Ver referencias",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.clickable { showReferences = !showReferences }
                    )
                    if (showReferences) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                            course.referencias.forEach { ref ->
                                Text(
                                    text = ref,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ref))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
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
}*/
