package com.edukrd.app.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.edukrd.app.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import androidx.core.app.NotificationCompat

// Modelo de datos de usuario, ajustado a Firestore
data class User(
    val name: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val sector: String = "",
    val phone: String = "",
    val email: String = "",
    val createdAt: com.google.firebase.Timestamp? = null,
    val notificationsEnabled: Boolean = false,
    val notificationFrequency: String = "Diaria"
)

/**
 * Worker para las notificaciones de recordatorio.
 */
class ReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        showNotification("Recordatorio EdukRD", "¡No pierdas el hábito de aprender sobre historia dominicana!")
        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "edukrd_reminders"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal en Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios EdukRD",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

/**
 * SettingsScreen: Muestra un resumen de datos del usuario y botones "Volver" y "Editar".
 * Al pulsar "Editar", se abre un dialog con los campos editables. Al guardar, se actualiza Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val userId = currentUser?.uid

    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userData by remember { mutableStateOf(User()) }

    // Almacenar email original para comparar si cambió
    var originalEmail by remember { mutableStateOf("") }

    // Para mostrar/ocultar el dialog de edición
    var showEditDialog by remember { mutableStateOf(false) }

    val dominicanBlue = Color(0xFF1565C0)

    // Cargar datos del usuario
    LaunchedEffect(userId) {
        if (userId == null) {
            errorMessage = "Usuario no autenticado."
            loading = false
            return@LaunchedEffect
        }
        try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) {
                userData = doc.toObject(User::class.java) ?: User()
                originalEmail = userData.email
            }
            loading = false
        } catch (e: Exception) {
            errorMessage = "Error al cargar datos: ${e.message}"
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", color = Color.White) },
                colors = topAppBarColors(containerColor = dominicanBlue)
            )
        }
    ) { innerPadding ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = errorMessage!!, color = Color.Red)
            }
        } else {
            // Muestra resumen de la configuración
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Datos Personales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Resumen
                Text("Nombre: ${userData.name}")
                Text("Apellido: ${userData.lastName}")
                Text("Fecha de Nacimiento: ${userData.birthDate}")
                Text("Sector: ${userData.sector}")
                Text("Teléfono: ${userData.phone}")
                Text("Correo: ${userData.email}")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Notificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val notifStatus = if (userData.notificationsEnabled) "Habilitadas" else "Deshabilitadas"
                Text("Estado: $notifStatus")
                if (userData.notificationsEnabled) {
                    Text("Frecuencia: ${userData.notificationFrequency}")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            // Volver al menú principal
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Volver")
                    }

                    Button(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = dominicanBlue)
                    ) {
                        Text("Editar")
                    }
                }
            }

            // Dialogo de edición
            if (showEditDialog) {
                EditSettingsDialog(
                    initialUserData = userData,
                    originalEmail = originalEmail,
                    onDismiss = { showEditDialog = false },
                    onSave = { updatedUserData ->
                        // Guardar en Firestore
                        if (userId != null) {
                            db.collection("users").document(userId)
                                .update(
                                    mapOf(
                                        "name" to updatedUserData.name,
                                        "lastName" to updatedUserData.lastName,
                                        "birthDate" to updatedUserData.birthDate,
                                        "sector" to updatedUserData.sector,
                                        "phone" to updatedUserData.phone,
                                        "email" to updatedUserData.email,
                                        "notificationsEnabled" to updatedUserData.notificationsEnabled,
                                        "notificationFrequency" to updatedUserData.notificationFrequency
                                    )
                                )
                                .addOnSuccessListener {
                                    // Actualizar Auth si cambió correo
                                    if (updatedUserData.email != originalEmail) {
                                        auth.currentUser?.updateEmail(updatedUserData.email)
                                            ?.addOnSuccessListener {
                                                Log.d("SettingsScreen", "Correo actualizado en Auth.")
                                            }
                                            ?.addOnFailureListener { e ->
                                                Log.e("SettingsScreen", "Error al actualizar el correo en Auth", e)
                                            }
                                    }
                                    // Programar o cancelar notificaciones
                                    if (updatedUserData.notificationsEnabled) {
                                        scheduleNotifications(context, updatedUserData.notificationFrequency)
                                    } else {
                                        cancelNotifications(context)
                                    }
                                    // Actualizar UI local
                                    userData = updatedUserData
                                    originalEmail = updatedUserData.email
                                    showEditDialog = false
                                }
                                .addOnFailureListener {
                                    Log.e("SettingsScreen", "Error al guardar cambios", it)
                                }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Dialogo flotante para editar los datos del usuario y su configuración de notificaciones.
 */
@Composable
fun EditSettingsDialog(
    initialUserData: User,
    originalEmail: String,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var tempUserData by remember { mutableStateOf(initialUserData) }

    AlertDialog(
        onDismissRequest = { /* Evitar cerrar si tocan fuera */ },
        title = { Text("Editar Configuración") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Campos personales
                OutlinedTextField(
                    value = tempUserData.name,
                    onValueChange = { tempUserData = tempUserData.copy(name = it) },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tempUserData.lastName,
                    onValueChange = { tempUserData = tempUserData.copy(lastName = it) },
                    label = { Text("Apellido") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tempUserData.birthDate,
                    onValueChange = { tempUserData = tempUserData.copy(birthDate = it) },
                    label = { Text("Fecha de Nacimiento") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tempUserData.sector,
                    onValueChange = { tempUserData = tempUserData.copy(sector = it) },
                    label = { Text("Sector") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tempUserData.phone,
                    onValueChange = { tempUserData = tempUserData.copy(phone = it) },
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tempUserData.email,
                    onValueChange = { tempUserData = tempUserData.copy(email = it) },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Notificaciones
                Text("Notificaciones", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Habilitar notificaciones", modifier = Modifier.weight(1f))
                    Switch(
                        checked = tempUserData.notificationsEnabled,
                        onCheckedChange = { tempUserData = tempUserData.copy(notificationsEnabled = it) }
                    )
                }
                if (tempUserData.notificationsEnabled) {
                    val opciones = listOf("Diaria", "Semanal", "Mensual")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Frecuencia: ")
                        Spacer(modifier = Modifier.width(8.dp))
                        opciones.forEach { opcion ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = (tempUserData.notificationFrequency == opcion),
                                    onClick = { tempUserData = tempUserData.copy(notificationFrequency = opcion) }
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
            Button(onClick = { onSave(tempUserData) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Programa notificaciones usando WorkManager según la frecuencia.
 */
fun scheduleNotifications(context: Context, frequency: String) {
    cancelNotifications(context)
    val repeatInterval = when (frequency) {
        "Diaria" -> 24L
        "Semanal" -> 24L * 7
        "Mensual" -> 24L * 30
        else -> 24L
    }
    val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(repeatInterval, TimeUnit.HOURS)
        .setInitialDelay(1, TimeUnit.MINUTES)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "EdukRDReminderWork",
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
    )
}

/**
 * Cancela las notificaciones programadas.
 */
fun cancelNotifications(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork("EdukRDReminderWork")
}
