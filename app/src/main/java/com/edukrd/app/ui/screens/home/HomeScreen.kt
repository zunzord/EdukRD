package com.edukrd.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.ui.components.GlobalStatsDialog
import com.edukrd.app.ui.components.LoadingPlaceholder
import com.edukrd.app.ui.components.MotivationalBubble
import com.edukrd.app.ui.components.ReportDialog
import com.edukrd.app.ui.screens.home.components.FullScreenCourseDetailSheet
import com.edukrd.app.ui.screens.home.components.TopHeader
import com.edukrd.app.ui.screens.home.components.TutorialOverlay
import com.edukrd.app.util.TutorialPreferenceHelper
import com.edukrd.app.viewmodel.CourseViewModel
import com.edukrd.app.viewmodel.ExamViewModel
import com.edukrd.app.viewmodel.UserViewModel
import kotlinx.coroutines.launch


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

    //estados para los reportes
    var showHelpDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

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
                            .clickable { showHelpDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Mostrar opciones de ayuda",
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
                                    // Otros elementos de la UI...
                                }
                            }
                        }
                    }
                }
            }

// Si el diálogo de estadísticas globales se debe mostrar.
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

// Diálogo de ayuda: pregunta si el usuario quiere ver el tutorial o realizar un reporte.
            if (showHelpDialog) {
                AlertDialog(
                    onDismissRequest = { showHelpDialog = false },
                    title = { Text("Ayuda") },
                    text = { Text("¿Qué deseas hacer?") },
                    confirmButton = {
                        Button(onClick = {
                            showHelpDialog = false
                            showTutorialOverlay = true
                        }) {
                            Text("Tutorial")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showHelpDialog = false
                            showReportDialog = true
                        }) {
                            Text("Reporte")
                        }
                    }
                )
            }

// Diálogo para enviar reporte.
            if (showReportDialog) {
                ReportDialog(
                    onDismiss = { showReportDialog = false }
                )
            }

// Overlay del tutorial (si corresponde).
            if (showTutorialOverlay) {
                TutorialOverlay(
                    onDismiss = { showTutorialOverlay = false }
                )
            }
            MotivationalBubble(
                message = "Pulsa aquí \nEMPECECMOS A APRENDER!",
                detailedDescription = "Desde esta pantalla tienes acceso a toda la aplicacion. Observa tu objetivo diario; este te indicará el promedio de examanes que debes completar, y así serás el 1ro en el ranking. \nPulsa en las imagenes, son cursos. Al completarlos, obtendras su medalla, así como monedas de recompenza"
            )
        }
    }    }