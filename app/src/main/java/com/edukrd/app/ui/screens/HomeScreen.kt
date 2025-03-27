package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import com.edukrd.app.viewmodel.ExamViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.edukrd.app.ui.components.LoadingPlaceholder
import com.edukrd.app.ui.components.AsyncImageWithShimmer
import androidx.compose.runtime.collectAsState
import com.edukrd.app.viewmodel.DailyTarget
import com.edukrd.app.viewmodel.UserGoalsState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.ceil
import com.edukrd.app.ui.components.GlobalStatsDialog
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource

@Composable
fun HomeScreen(navController: NavController) {
    val userViewModel: UserViewModel = hiltViewModel()
    val courseViewModel: CourseViewModel = hiltViewModel()
    val examViewModel: ExamViewModel = hiltViewModel()

    // Usamos el nuevo StateFlow con las metas y progreso (UserGoalsState)
    val userGoalsState by examViewModel.userGoalsState.collectAsState()

    // Cargamos tanto el antiguo método (para la lógica de streak, si es que aún lo necesitas) como el nuevo cálculo de metas
    LaunchedEffect(Unit) {
        examViewModel.loadDailyStreakTarget()
        examViewModel.loadUserGoals()
        userViewModel.loadCurrentUserData()
        courseViewModel.loadCoursesAndProgress()
    }

    val userData by userViewModel.userData.collectAsState()
    val userLoading by userViewModel.loading.collectAsState()
    val courses by courseViewModel.courses.collectAsState()
    val passedCourseIds by courseViewModel.passedCourseIds.collectAsState()
    val coinRewards by courseViewModel.coinRewards.collectAsState()
    val coursesLoading by courseViewModel.loading.collectAsState()
    val courseError by courseViewModel.error.collectAsState()

    val dailyData by examViewModel.dailyGraphData.collectAsState()
    val weeklyData by examViewModel.weeklyGraphData.collectAsState()
    val monthlyData by examViewModel.monthlyGraphData.collectAsState()

    // Estado para controlar la visualización del diálogo de resumen de metas
    val showDialog = remember { mutableStateOf(false) }

    // Estado para el ModalBottomSheet de detalle de cursos
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
                    userLoading || coursesLoading -> LoadingPlaceholder()
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
                            // BannerSection: muestra la imagen de banner, saludo y la tarjeta de meta diaria
                            val bannerImageUrl = courses.firstOrNull()?.imageUrl ?: ""
                            BannerSection(
                                bannerUrl = bannerImageUrl,
                                userName = userData?.name ?: "User",
                                userGoalsState = userGoalsState,
                                onDailyTargetClick = { showDialog.value = true }
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

    // Diálogo modal para mostrar el resumen de metas (semanal, mensual y el progreso global)
    if (showDialog.value) {
        GlobalStatsDialog(
            onDismiss = { showDialog.value = false },
            dailyCurrent = userGoalsState.dailyCurrent,
            dailyTarget = userGoalsState.dailyTarget,
            weeklyCurrent = userGoalsState.weeklyCurrent,
            weeklyTarget = userGoalsState.weeklyTarget,
            monthlyCurrent = userGoalsState.monthlyCurrent,
            monthlyTarget = userGoalsState.monthlyTarget,
            dailyData = dailyData,
            weeklyData = weeklyData,
            monthlyData = monthlyData
        )
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
    userName: String,
    userGoalsState: UserGoalsState,
    onDailyTargetClick: () -> Unit
) {
    val context = LocalContext.current
    Column {
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
        // Se invoca DailyProgressCard debajo del banner
        DailyProgressCard(
            dailyCurrent = userGoalsState.dailyCurrent,
            dailyTarget = userGoalsState.dailyTarget,
            onDailyTargetClick = onDailyTargetClick
        )
    }
}

@Composable
fun DailyProgressCard(
    dailyCurrent: Int,
    dailyTarget: Int,
    onDailyTargetClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onDailyTargetClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Meta diaria", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(12.dp))

            val ratio = if (dailyTarget > 0) dailyCurrent.toFloat() / dailyTarget else 0f
            val barColor = MaterialTheme.colorScheme.primary.toArgb()

            // Contenedor donde se superpone la barra y el texto a la derecha
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                // 1) BarChart con barra y sombra gris
                AndroidView(
                    modifier = Modifier.fillMaxSize()
                        .requiredHeight(48.dp)         // barra dibujada doble grosor
                        .offset(y = (-12).dp),         // desplaza mitad arriba para centrar
                    factory = { context ->
                        BarChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false

                            // Dibujar la "sombra" para la parte no completada
                            setDrawBarShadow(false)
                            setDrawValueAboveBar(false)

                            axisRight.isEnabled = false
                            axisLeft.isEnabled = false
                            xAxis.isEnabled = false

                            setDrawGridBackground(false)
                            setDrawBorders(false)
                            setBackgroundColor(Color.Transparent.toArgb())
                        }
                    },
                    update = { chart ->
                        val entry = BarEntry(0f, ratio)
                        val dataSet = BarDataSet(listOf(entry), "").apply {
                            color = barColor
                            //barShadowColor = Color.LightGray.toArgb()
                            setDrawValues(false)
                        }
                        // Usamos barWidth = 1f para que la barra ocupe todo el ancho
                        val barData = BarData(dataSet).apply {
                            barWidth = 1f
                        }
                        chart.data = barData
                        chart.invalidate()
                    }
                )

                // 2) Texto + icono superpuestos a la derecha
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$dailyCurrent/$dailyTarget",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (dailyCurrent >= dailyTarget) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.star),
                            tint = Color.Unspecified,
                            contentDescription = "Meta diaria alcanzada",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
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
                AsyncImageWithShimmer(
                    url = course.medalla,
                    contentDescription = "Medalla",
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.TopStart)
                        .padding(8.dp)
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
                AsyncImageWithShimmer(
                    url = course.imageUrl,
                    contentDescription = course.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
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
                text = "$coinReward monedas",
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
