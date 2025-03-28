package com.edukrd.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.ui.components.GlobalStatsDialog
import com.edukrd.app.ui.components.LoadingPlaceholder
import com.edukrd.app.ui.screens.home.components.BottomNavigationBar
import com.edukrd.app.ui.screens.home.components.FullScreenCourseDetailSheet
import com.edukrd.app.ui.screens.home.components.TopHeader
import com.edukrd.app.ui.screens.home.BannerSection
import com.edukrd.app.ui.screens.home.CourseGrid
import kotlinx.coroutines.launch
import androidx.compose.material.ModalBottomSheetLayout
import com.edukrd.app.viewmodel.UserViewModel
import com.edukrd.app.viewmodel.CourseViewModel
import com.edukrd.app.viewmodel.ExamViewModel
import com.edukrd.app.models.UserGoalsState

@Composable
fun HomeScreen(navController: NavController) {
    // Inyectamos los tres ViewModels originales
    val userViewModel: UserViewModel = hiltViewModel()
    val courseViewModel: CourseViewModel = hiltViewModel()
    val examViewModel: ExamViewModel = hiltViewModel()

    // Cargar datos al iniciar
    LaunchedEffect(Unit) {
        userViewModel.loadCurrentUserData()
        courseViewModel.loadCoursesAndProgress()
        examViewModel.loadDailyStreakTarget() // Si usas este método
        examViewModel.loadUserGoals()
    }

    // Observamos los estados de cada ViewModel
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
    val userGoalsState by examViewModel.userGoalsState.collectAsState()

    // Unificamos el estado de carga
    val isLoading = userLoading || coursesLoading

    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    // Estado para el ModalBottomSheet que muestra el detalle completo del curso
    var selectedCourse by remember { mutableStateOf<Any?>(null) }
    // Estado para el diálogo de estadísticas globales (metas)
    var showGlobalStatsDialog by remember { mutableStateOf(false) }

    // Layout con ModalBottomSheet para mostrar el detalle de un curso
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            // Solo mostramos el contenido si se ha seleccionado un curso
            (selectedCourse as? com.edukrd.app.models.Course)?.let { course ->
                FullScreenCourseDetailSheet(
                    course = course,
                    isCompleted = passedCourseIds.contains(course.id),
                    coinReward = coinRewards[course.id] ?: 0,
                    onClose = {
                        coroutineScope.launch { sheetState.hide() }
                        selectedCourse = null
                    },
                    onTakeCourse = {
                        navController.navigate("course/${course.id}")
                        coroutineScope.launch { sheetState.hide() }
                        selectedCourse = null
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
                        // Aquí se ejecutaría la lógica de logout, por ejemplo:
                        // FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            },
            bottomBar = { BottomNavigationBar(navController, currentRoute = "home") }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    isLoading -> {
                        LoadingPlaceholder()
                    }
                    courseError != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = courseError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    else -> {
                        // Contenido principal de la pantalla Home
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            Column {
                                // BannerSection: incluye el banner y la barra de progreso diaria
                                BannerSection(
                                    bannerUrl = courses.firstOrNull()?.imageUrl ?: "",
                                    userName = userData?.name ?: "User",
                                    userGoalsState = userGoalsState,
                                    onDailyTargetClick = { showGlobalStatsDialog = true }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                // CourseGrid: muestra los cursos en un LazyRow
                                CourseGrid(
                                    courses = courses,
                                    passedCourseIds = passedCourseIds,
                                    coinRewards = coinRewards,
                                    onCourseClick = { course ->
                                        selectedCourse = course
                                        coroutineScope.launch { sheetState.show() }
                                    }
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo modal para mostrar las estadísticas globales (resumen de metas)
    if (showGlobalStatsDialog) {
        GlobalStatsDialog(
            onDismiss = { showGlobalStatsDialog = false },
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
