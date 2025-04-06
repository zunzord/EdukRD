package com.edukrd.app.ui.screens.home

import android.util.Log
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
import androidx.compose.ui.layout.positionInRoot
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
import com.edukrd.app.ui.screens.home.components.TutorialItem
import com.edukrd.app.util.TutorialPreferenceHelper
import androidx.hilt.navigation.compose.hiltViewModel
import com.edukrd.app.viewmodel.CourseViewModel
import com.edukrd.app.viewmodel.ExamViewModel
import com.edukrd.app.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.ModalBottomSheetLayout
import com.edukrd.app.ui.screens.home.DailyProgressBar
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, navMedallasOffset: Offset,navTiendaOffset: Offset,navRankingOffset: Offset) {
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




    // Variable para almacenar la posición del contenedor raíz (para conversión de coordenadas)
    var rootOffset by remember { mutableStateOf(Offset.Zero) }

    // Activación automática del tutorial:
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
        // Contenedor raíz que captura su posición global para la conversión de coordenadas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    // Guardamos la posición del contenedor raíz en coordenadas de ventana
                    rootOffset = coordinates.localToWindow(Offset.Zero)
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
                        onSettingsIconPosition = { offset ->
                            settingsOffset = offset
                        },
                        onLogoutIconPosition = { offset ->
                            logoutOffset = offset
                        }
                    )
                },
                /*bottomBar = {
                    BottomNavigationBar(
                        navController,
                        currentRoute = "home",
                        onMedalsIconPosition = { offset ->
                            navMedallasOffset = offset
                        },
                        onStoreIconPosition = { offset ->
                            navTiendaOffset = offset
                        },
                        onRankingIconPosition = { offset ->
                            navRankingOffset = offset
                        }
                    )
                },*/
                floatingActionButton = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .clickable { showTutorialOverlay = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Mostrar tutorial",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
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
                                    userName = userData?.name ?: "User",
                                    userGoalsState = userGoalsState,
                                    onDailyTargetClick = { showGlobalStatsDialog = true },
                                    onBannerIconPosition = { offset ->
                                        bannerOffset = offset
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                // CourseGrid
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

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {



                                }
                            }
                        }
                    }
                }
            }
        }
    }


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


/*    val density = LocalDensity.current


    val convertOffset: (Offset) -> Offset = { measuredOffset ->
        measuredOffset - rootOffset





    }

    val medallasArrowX = with(density) { convertOffset(navMedallasOffset).x.toDp().value.toInt() }
    val medallasArrowY = with(density) { convertOffset(navMedallasOffset).y.toDp().value.toInt() }
    Log.d("TutorialStep", "Medallas Arrow - X: $medallasArrowX, Y: $medallasArrowY")


    val tutorialSteps = listOf(
        TutorialStep(
            id = 1,
            title = "Configuración",
            description = "Toca aquí para acceder a la configuración de la app y modificar tus preferencias.",
            targetAlignment = Alignment.TopStart,
           // arrowAlignment = Alignment.TopStart,
            arrowOffsetX = with(density) { convertOffset(settingsOffset).x.toDp().value.toInt() },
            arrowOffsetY = with(density) { convertOffset(settingsOffset).y.toDp().value.toInt() },
            bubbleOffsetX = 0,
            bubbleOffsetY = 48
        ),
        TutorialStep(
            id = 2,
            title = "Cerrar Sesión",
            description = "Toca aquí para salir de la aplicación.",
            targetAlignment = Alignment.TopEnd,
            arrowAlignment = Alignment.TopEnd,
            arrowOffsetX = with(density) { convertOffset(logoutOffset).x.toDp().value.toInt() },
            arrowOffsetY = with(density) { convertOffset(logoutOffset).y.toDp().value.toInt() },
            bubbleOffsetX = 0,
            bubbleOffsetY = 48
        ),
        TutorialStep(
            id = 3,
            title = "Barra de Objetivos",
            description = "Toca aquí para ver tus objetivos diarios, semanales y mensuales.",
            targetAlignment = Alignment.TopCenter,
            arrowAlignment = Alignment.TopCenter,
            arrowOffsetX = with(density) { convertOffset(bannerOffset).x.toDp().value.toInt() },
            arrowOffsetY = with(density) { (convertOffset(bannerOffset).y.toDp().value.toInt() + 24) },
            bubbleOffsetX = 0,
            bubbleOffsetY = 48
        ),
        TutorialStep(
            id = 4,
            title = "Medallas",
            description = "Presiona este botón para ver tu medallero y logros.",
            targetAlignment = Alignment.Center,
            arrowAlignment = Alignment.BottomCenter,
            arrowOffsetX = with(density) { convertOffset(navMedallasOffset).x.toDp().value.toInt() },
            arrowOffsetY = with(density) { convertOffset(navMedallasOffset).y.toDp().value.toInt() },
            bubbleOffsetX = 0,
            bubbleOffsetY = 10
        ),
        TutorialStep(
            id = 5,
            title = "Tienda",
            description = "Accede a la tienda para canjear tus monedas.",
            targetAlignment = Alignment.Center,
            arrowAlignment = Alignment.BottomCenter,
            arrowOffsetX = with(density) { convertOffset(navTiendaOffset).x.toDp().value.toInt() },
            arrowOffsetY = with(density) { convertOffset(navTiendaOffset).y.toDp().value.toInt() },
            bubbleOffsetX = 0,
            bubbleOffsetY = 10
        ),


        TutorialStep(
            id = 6,
            title = "Ranking",
            description = "Consulta el ranking para ver tu posición.",
            targetAlignment = Alignment.Center,
            arrowAlignment = Alignment.BottomCenter,
            arrowOffsetX = with(density) { convertOffset(navRankingOffset).x.toDp().value.toInt() },
            arrowOffsetY = with(density) { convertOffset(navRankingOffset).y.toDp().value.toInt() },
            bubbleOffsetX = 0,
            bubbleOffsetY = 10
        )
    )
*/
    // Overlay tutorial secuencial: se muestra sobre HomeScreen.
    if (showTutorialOverlay) {
        TutorialOverlay(

            onDismiss = { showTutorialOverlay = false }
        )
    }
}
