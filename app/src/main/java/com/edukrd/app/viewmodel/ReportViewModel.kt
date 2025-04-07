package com.edukrd.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.models.Report
import com.edukrd.app.repository.ReportRepository
import com.edukrd.app.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // Estado para el resultado de la operación de reporte:
    // Pair(true, "mensaje") para éxito, Pair(false, "mensaje") para error.
    private val _submitResult = MutableStateFlow<Pair<Boolean, String>?>(null)
    val submitResult: StateFlow<Pair<Boolean, String>?> = _submitResult

    /**
     * Envía un reporte (sugerencia o incidencia).
     * El usuario solo suministra el tipo y el mensaje.
     * Los demás campos se completan automáticamente a partir de su perfil.
     */
    fun submitReport(type: String, message: String) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _submitResult.value = Pair(false, "Usuario no autenticado")
                return@launch
            }

            // Obtén la data del usuario autenticado (por ejemplo, correo, teléfono y sector)
            val userData = userRepository.getUserData(currentUser.uid)
            if (userData == null) {
                _submitResult.value = Pair(false, "No se encontraron datos de usuario")
                return@launch
            }

            // Construye el objeto Report completando los campos faltantes
            val report = Report(
                reportId = "", // Será generado por Firestore.
                userId = currentUser.uid,
                email = userData.email,   // Obtenido del perfil del usuario
                phone = userData.phone,   // Obtenido del perfil del usuario
                sector = userData.sector, // Obtenido del perfil del usuario
                type = type,
                message = message,
                status = "open",
                createdAt = com.google.firebase.Timestamp.now()
            )

            // Llama al repositorio para enviar el reporte.
            val success = reportRepository.submitReport(report)
            if (success) {
                _submitResult.value = Pair(true, "Reporte enviado exitosamente.")
            } else {
                _submitResult.value = Pair(false, "Límite diario alcanzado o error al enviar el reporte.")
            }
        }
    }

    /**
     * Resetea el resultado del reporte.
     */
    fun resetSubmitResult() {
        _submitResult.value = null
    }
}
