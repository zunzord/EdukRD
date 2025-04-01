package com.edukrd.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.edukrd.app.models.UserGoalsState
import com.edukrd.app.ui.components.GlobalStatsDialog
import com.edukrd.app.ui.components.LoadingPlaceholder
import com.edukrd.app.ui.screens.home.BannerSection
import com.edukrd.app.ui.screens.home.CourseGrid
import com.edukrd.app.ui.screens.home.components.BottomNavigationBar
import com.edukrd.app.ui.screens.home.components.FullScreenCourseDetailSheet
import com.edukrd.app.ui.screens.home.components.TopHeader
import com.edukrd.app.ui.screens.home.components.TutorialOverlay
import com.edukrd.app.ui.screens.home.components.TutorialStep
import com.edukrd.app.util.TutorialPreferenceHelper
import androidx.hilt.navigation.compose.hiltViewModel
import com.edukrd.app.viewmodel.CourseViewModel
import com.edukrd.app.viewmodel.ExamViewModel
import com.edukrd.app.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.ui.layout.positionInWindow
import android.util.Log
import com.edukrd.app.ui.screens.home.DailyProgressBar




@OptIn(ExperimentalMaterial3Api::class)
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
        examViewModel.loadDailyStreakTarget()
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

    // Estado para el ModalBottomSheet que muestra el detalle de un curso
    var selectedCourse by remember { mutableStateOf<Any?>(null) }
    // Estado para el diálogo de estadísticas globales (metas)
    var showGlobalStatsDialog by remember { mutableStateOf(false) }

    // Estados para el tutorial overlay:
    // showTutorialOverlay controla su visualización, y hasShownTutorial evita reactivarlo en la misma sesión.
    var showTutorialOverlay by remember { mutableStateOf(false) }
    var hasShownTutorial by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // Variables para medir las posiciones de elementos clave
    var settingsOffset by remember { mutableStateOf(Offset.Zero) }
    var logoutOffset by remember { mutableStateOf(Offset.Zero) }
    var bannerOffset by remember { mutableStateOf(Offset.Zero) }
    var navMedallasOffset by remember { mutableStateOf(Offset.Zero) }
    var navTiendaOffset by remember { mutableStateOf(Offset.Zero) }
    var navRankingOffset by remember { mutableStateOf(Offset.Zero) }

    // Activación automática del tutorial:
    // Si el usuario ya completó el onboarding (primerAcceso == false) y el flag persistente indica que aún no se mostró,
    // se activa.
    LaunchedEffect(userData) {
        userData?.let { user ->
            if (!user.primerAcceso && !TutorialPreferenceHelper.isTutorialShown(context) && !hasShownTutorial) {
                showTutorialOverlay = true
                hasShownTutorial = true
                TutorialPreferenceHelper.setTutorialShown(context, true)
            }
        }
    }

    // Layout con ModalBottomSheet para mostrar el detalle del curso
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
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
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onSettingsIconPosition = { offset -> settingsOffset = offset },
                    onLogoutIconPosition = { offset -> logoutOffset = offset }
                )
            },
            bottomBar = { BottomNavigationBar(navController, currentRoute = "home",onMedalsIconPosition = { offset -> navMedallasOffset = offset },
                onStoreIconPosition = { offset -> navTiendaOffset = offset },
                onRankingIconPosition = { offset -> navRankingOffset = offset }) },
            floatingActionButton = {
                // Botón tutotial.
                IconButton(
                    onClick = { showTutorialOverlay = true },
                    modifier = Modifier.background(Color.White, shape = MaterialTheme.shapes.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Mostrar tutorial",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Start
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
                        // Contenido principal de HomeScreen.
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            // BannerSection con medición de posición.

                                BannerSection(

                                    bannerUrl = courses.firstOrNull()?.imageUrl ?: "",
                                    userName = userData?.name ?: "User",
                                    userGoalsState = userGoalsState,
                                    onDailyTargetClick = { showGlobalStatsDialog = true },
                                    onBannerIconPosition = { offset: Offset -> bannerOffset = offset }
                                )

                            Spacer(modifier = Modifier.height(16.dp))
                            // CourseGrid (sin medición adicional, pues normalmente se muestra completo)
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
                            // Ejemplo: Medición de botones de la barra de navegación (si no se miden en BottomNavigationBar)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {

                                    Text("Medallas", modifier = Modifier.padding(8.dp))



                                    Text("Tienda", modifier = Modifier.padding(8.dp))


                                    Text("Ranking", modifier = Modifier.padding(8.dp))

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

    // Construcción de la lista de pasos del tutorial utilizando las posiciones medidas.

    val density = LocalDensity.current

    val tutorialSteps = listOf(
        TutorialStep(
            id = 1,
            title = "Configuración",
            description = "Toca aquí para acceder a la configuración de la app y modificar tus preferencias.",
            targetAlignment = Alignment.TopStart,
            arrowAlignment = Alignment.TopStart,
            Log.d("TutorialOffsets", "Settings arrowOffsetX=${with(density) { settingsOffset.x.toDp().value.toInt() }}, arrowOffsetY=${with(density) { settingsOffset.y.toDp().value.toInt() }}"),
            bubbleOffsetX = 0,
            bubbleOffsetY = 48
        ),
        TutorialStep(
            id = 2,
            title = "Cerrar Sesión",
            description = "Toca aquí para salir de la aplicación.",
            targetAlignment = Alignment.TopEnd,
            arrowAlignment = Alignment.TopEnd,
            Log.d("TutorialOffsets", "Logout arrowOffsetX=${with(density) { logoutOffset.x.toDp().value.toInt() }}, arrowOffsetY=${with(density) { logoutOffset.y.toDp().value.toInt() }}"),
            bubbleOffsetX = 0,
            bubbleOffsetY = 48
        ),
        TutorialStep(
            id = 3,
            title = "Barra de Objetivos",
            description = "Toca aquí para ver tus objetivos diarios, semanales y mensuales.",
            targetAlignment = Alignment.TopCenter,
            arrowAlignment = Alignment.Center,
            Log.d("TutorialOffsets", "Medallas arrowOffsetX=${with(density) { bannerOffset.x.toDp().value.toInt() }}, arrowOffsetY=${with(density) { bannerOffset.y.toDp().value.toInt() +24}}"),
            /*arrowOffsetX=with(density) { bannerOffset.x.toDp().value.toInt() },
            arrowOffsetY=with(density) { bannerOffset.y.toDp().value.toInt() },*/
            bubbleOffsetX = 0,
            bubbleOffsetY = 48
        ),
        TutorialStep(
            id = 4,
            title = "Medallas",
            description = "Presiona este botón para ver tu medallero y logros.",
            targetAlignment = Alignment.BottomStart,
            arrowAlignment = Alignment.BottomStart,
            Log.d("TutorialOffsets", "Medallas arrowOffsetX=${with(density) { navMedallasOffset.x.toDp().value.toInt() }}, arrowOffsetY=${with(density) { navMedallasOffset.y.toDp().value.toInt() }}"),
            bubbleOffsetX = 0,
            bubbleOffsetY = -56
        ),
        TutorialStep(
            id = 5,
            title = "Tienda",
            description = "Accede a la tienda para canjear tus monedas.",
            targetAlignment = Alignment.BottomCenter,
            arrowAlignment = Alignment.BottomCenter,
            Log.d("TutorialOffsets", "Tienda arrowOffsetX=${with(density) { navTiendaOffset.x.toDp().value.toInt() }}, arrowOffsetY=${with(density) { navTiendaOffset.y.toDp().value.toInt() }}"),
            bubbleOffsetX = 0,
            bubbleOffsetY = -56
        ),
        TutorialStep(
            id = 6,
            title = "Ranking",
            description = "Consulta el ranking para ver tu posición.",
            targetAlignment = Alignment.BottomEnd,
            arrowAlignment = Alignment.BottomEnd,
            Log.d("TutorialOffsets", "Ranking arrowOffsetX=${with(density) { navRankingOffset.x.toDp().value.toInt() }}, arrowOffsetY=${with(density) { navRankingOffset.y.toDp().value.toInt() }}"),
            bubbleOffsetX = 0,
            bubbleOffsetY = -56
        )
    )

    // Overlay tutorial secuencial: se muestra sobre HomeScreen.
    if (showTutorialOverlay) {
        TutorialOverlay(
            tutorialSteps = tutorialSteps,
            onDismiss = { showTutorialOverlay = false }
        )
    }
}
