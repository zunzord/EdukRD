package com.edukrd.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.models.User
import com.edukrd.app.viewmodel.AuthViewModel
import com.edukrd.app.viewmodel.ThemeViewModel
import com.edukrd.app.viewmodel.UserViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import com.edukrd.app.ui.components.DotLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel  // Se recibe el ThemeViewModel desde NavGraph
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()

    val userData by userViewModel.userData.collectAsState()
    val loading by userViewModel.loading.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }

    // Carga datos del usuario
    LaunchedEffect(Unit) {
        userViewModel.loadCurrentUserData()
    }

    // TopAppBar con flecha para volver e ícono de lápiz a la derecha
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color.White
                        )
                    }
                },
                colors = topAppBarColors(containerColor = Color(0xFF1565C0))
            )
        }
    ) { innerPadding ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                DotLoadingIndicator(modifier = Modifier.size(56.dp))
            }
        } else {
            val currentUserData = userData
            if (currentUserData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error al cargar los datos del usuario", color = Color.Red)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Datos Personales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Nombre: ${currentUserData.name}")
                    Text("Apellido: ${currentUserData.lastName}")
                    Text("Fecha de Nacimiento: ${currentUserData.birthDate}")
                    Text("Sector: ${currentUserData.sector}")
                    Text("Teléfono: ${currentUserData.phone}")
                    Text("Correo: ${currentUserData.email}")
                    Text("Tema: ${if (currentUserData.themePreference == "dark") "Oscuro" else "Claro"}")

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Notificaciones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val notifStatus = if (currentUserData.notificationsEnabled) "Habilitadas" else "Deshabilitadas"
                    Text("Estado: $notifStatus")
                    if (currentUserData.notificationsEnabled) {
                        Text("Frecuencia: ${currentUserData.notificationFrequency}")
                    }
                }

                // Diálogo de edición
                if (showEditDialog) {
                    EditSettingsDialog(
                        initialUserData = currentUserData,
                        onDismiss = { showEditDialog = false },
                        onSave = { updatedUser ->
                            userViewModel.updateCurrentUserData(updatedUser) { success ->
                                if (success) {
                                    // Sincronizar tema inmediatamente
                                    themeViewModel.updateThemePreference(updatedUser.themePreference)
                                }
                                showEditDialog = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSettingsDialog(
    initialUserData: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var tempUser by remember { mutableStateOf(initialUserData) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Configuración") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tempUser.name,
                    onValueChange = { tempUser = tempUser.copy(name = it) },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tempUser.lastName,
                    onValueChange = { tempUser = tempUser.copy(lastName = it) },
                    label = { Text("Apellido") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tempUser.birthDate,
                    onValueChange = { tempUser = tempUser.copy(birthDate = it) },
                    label = { Text("Fecha de Nacimiento") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tempUser.sector,
                    onValueChange = { tempUser = tempUser.copy(sector = it) },
                    label = { Text("Sector") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tempUser.phone,
                    onValueChange = { tempUser = tempUser.copy(phone = it) },
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tempUser.email,
                    onValueChange = { tempUser = tempUser.copy(email = it) },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Preferencia de Tema",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (tempUser.themePreference == "light"),
                        onClick = { tempUser = tempUser.copy(themePreference = "light") }
                    )
                    Text("Claro")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = (tempUser.themePreference == "dark"),
                        onClick = { tempUser = tempUser.copy(themePreference = "dark") }
                    )
                    Text("Oscuro")
                }

                Text(
                    "Notificaciones",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Habilitar notificaciones", modifier = Modifier.weight(1f))
                    Switch(
                        checked = tempUser.notificationsEnabled,
                        onCheckedChange = { tempUser = tempUser.copy(notificationsEnabled = it) }
                    )
                }
                if (tempUser.notificationsEnabled) {
                    val opciones = listOf("Diaria", "Semanal", "Mensual")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Frecuencia: ")
                        Spacer(modifier = Modifier.width(8.dp))
                        opciones.forEach { opcion ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = (tempUser.notificationFrequency == opcion),
                                    onClick = { tempUser = tempUser.copy(notificationFrequency = opcion) }
                                )
                                Text(opcion)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(tempUser) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Cancelar")
            }
        }
    )
}
