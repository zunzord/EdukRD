package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.edukrd.app.R
import com.edukrd.app.models.Course
import com.edukrd.app.viewmodel.CourseViewModel
import com.edukrd.app.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Composable
fun HomeScreen(navController: NavController) {
    val userViewModel: UserViewModel = hiltViewModel()
    val courseViewModel: CourseViewModel = hiltViewModel()

    val userData by userViewModel.userData.collectAsState()
    val userLoading by userViewModel.loading.collectAsState()
    val courses by courseViewModel.courses.collectAsState()
    val passedCourseIds by courseViewModel.passedCourseIds.collectAsState()
    val coinRewards by courseViewModel.coinRewards.collectAsState()
    val coursesLoading by courseViewModel.loading.collectAsState()
    val courseError by courseViewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        userViewModel.loadCurrentUserData()
        courseViewModel.loadCoursesAndProgress()
    }
    val loading = userLoading || coursesLoading

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val coroutineScope = rememberCoroutineScope()
    var selectedCourse by remember { mutableStateOf<Course?>(null) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            selectedCourse?.let { course ->
                FullScreenCourseDetailSheet(
                    course = course,
                    isCompleted = passedCourseIds.contains(course.id),
                    coinReward = coinRewards[course.id] ?: 0,
                    onClose = {
                        coroutineScope.launch { sheetState.hide() }
                        selectedCourse = null
                    },
                    onTakeCourse = {
                        courseViewModel.startCourse(course.id)
                        coroutineScope.launch {
                            sheetState.hide()
                            selectedCourse = null
                        }
                        navController.navigate("course/${course.id}")
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopHeader(
                    userName = userData?.name ?: "User",
                    onSettingsClick = { navController.navigate("settings") },
                    onLogoutClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            },
            bottomBar = { BottomNavigationBar(navController) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    courseError != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = courseError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            // Mostramos la imagen única del primer curso en el banner
                            val bannerImageUrl = courses.firstOrNull()?.imageUrl ?: ""
                            BannerSection(
                                bannerUrl = bannerImageUrl,
                                userName = userData?.name ?: "User"
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(courses) { course ->
                                    val isCompleted = passedCourseIds.contains(course.id)
                                    ExperienceCard(
                                        course = course,
                                        isCompleted = isCompleted,
                                        onClick = {
                                            selectedCourse = course
                                            coroutineScope.launch { sheetState.show() }
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopHeader(
    userName: String,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EDUKRD",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun BannerSection(
    bannerUrl: String,
    userName: String
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.fondo),
            contentDescription = null,
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
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { /* Home ya está activa */ },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_course),
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("medals") },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_medal),
                    contentDescription = "Medals"
                )
            },
            label = { Text("Medals") },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("store") },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_store),
                    contentDescription = "Store"
                )
            },
            label = { Text("Store") },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("ranking") },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ranking),
                    contentDescription = "Ranking"
                )
            },
            label = { Text("Ranking") },
            alwaysShowLabel = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperienceCard(
    course: Course,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(200.dp)
            .height(240.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Usamos directamente course.imageUrl (campo string)
            val coverUrl = course.imageUrl
            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(coverUrl)
                        .crossfade(true)
                        .size(400, 400)
                        .placeholder(R.drawable.ic_course)
                        .error(R.drawable.ic_course)
                        .build(),
                    contentDescription = course.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
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
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                )
            }
            if (isCompleted && course.medalla.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(course.medalla)
                        .crossfade(true)
                        .size(200, 200)
                        .placeholder(R.drawable.ic_course)
                        .error(R.drawable.ic_course)
                        .build(),
                    contentDescription = "Medalla",
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun FullScreenCourseDetailSheet(
    course: Course,
    isCompleted: Boolean,
    coinReward: Int,
    onClose: () -> Unit,
    onTakeCourse: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            val coverUrl = course.imageUrl
            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(coverUrl)
                        .crossfade(true)
                        .size(800, 400)
                        .placeholder(R.drawable.ic_course)
                        .error(R.drawable.ic_course)
                        .build(),
                    contentDescription = course.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
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
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            val reward = if (!isCompleted) course.recompenza else course.recompenzaExtra
            Text(
                text = "$coinReward coins",
                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row {
                repeat(5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = Color(0xFFFFD700)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "(5.0)",
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
}
