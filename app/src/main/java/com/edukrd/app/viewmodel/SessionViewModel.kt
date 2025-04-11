package com.edukrd.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.data.preferences.SessionPreferences
import com.edukrd.app.models.Session
import com.edukrd.app.repository.SessionRepository
import com.edukrd.app.usecase.GetDeviceIdUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val auth: FirebaseAuth,
    private val sessionPrefs: SessionPreferences, // Maneja el almacenamiento local del sessionId.
    private val getDeviceIdUseCase: GetDeviceIdUseCase
) : ViewModel() {

    // Estado de la sesión actual: será null si no hay sesión activa.
    private val _sessionState = MutableStateFlow<Session?>(null)
    val sessionState: StateFlow<Session?> = _sessionState.asStateFlow()

    // Canal para notificar a la UI sobre conflicto de sesión (por ejemplo, sesión activa en otro dispositivo).
    private val _sessionConflictEvent = Channel<String>(Channel.BUFFERED)
    val sessionConflictEvent = _sessionConflictEvent.receiveAsFlow()

    init {
        // Si en preferencias ya existe un sessionId, inicia un listener para esa sesión de la subcolección correspondiente.
        viewModelScope.launch {
            sessionPrefs.getSessionId()?.let { storedSessionId ->
                auth.currentUser?.uid?.let { uid ->
                    Log.d("SessionViewModel", "Iniciando listener para sessionId: $storedSessionId en uid: $uid")
                    sessionRepository.getSessionFlow(uid, storedSessionId).collect { session ->
                        _sessionState.value = session
                        Log.d("SessionViewModel", "Listener detectó sesión: ${session?.sessionId} activa: ${session?.active}")
                        // Si la sesión se vuelve inactiva, se fuerza el logout.
                        if (session != null && !session.active) {
                            forceLogout()
                        }
                    }
                }
            }
        }
    }

    /**
     * Lógica de inicio de sesión robusta:
     * - Obtiene el deviceId actual.
     * - Consulta si existe una sesión activa en Firestore para el usuario en su subcolección.
     *   - Si existe y corresponde al dispositivo actual, actualiza la sesión.
     *   - Si existe pero pertenece a otro dispositivo, se notifica un conflicto.
     *   - Si no existe, crea una nueva sesión.
     */
    fun createNewSession() {
        viewModelScope.launch {
            val currentUser = auth.currentUser ?: run {
                Log.e("SessionViewModel", "No hay usuario autenticado")
                return@launch
            }
            val uid = currentUser.uid
            val deviceId = getDeviceIdUseCase() // Obtiene el deviceId.
            Log.d("SessionViewModel", "DeviceId obtenido: $deviceId")

            // Consulta si existe alguna sesión activa en la subcolección del usuario.
            val activeSession = sessionRepository.getActiveSession(uid)
            val storedSessionId = sessionPrefs.getSessionId()

            when {
                activeSession != null -> {
                    if (storedSessionId != null &&
                        storedSessionId == activeSession.sessionId &&
                        activeSession.deviceId == deviceId
                    ) {
                        // La sesión activa corresponde a este dispositivo: actualiza el timestamp y continúa.
                        sessionRepository.updateSessionLastUpdate(activeSession.sessionId, uid)
                        _sessionState.value = activeSession
                        Log.d("SessionViewModel", "Actualizando sesión existente: ${activeSession.sessionId}")
                    } else {
                        // Existe sesión activa pero en otro dispositivo.
                        Log.d("SessionViewModel", "Conflicto: sesión activa en otro dispositivo detectada")
                        _sessionConflictEvent.send("Ya existe una sesión activa en otro dispositivo. ¿Deseas cerrar las sesiones previas y continuar?")
                    }
                }
                else -> {
                    // No existe sesión activa; se crea una nueva sesión en la subcolección.
                    val newSession = sessionRepository.createOrUpdateSession(deviceId)
                    if (newSession != null) {
                        Log.d("SessionViewModel", "Nueva sesión creada: ${newSession.sessionId}")
                        _sessionState.value = newSession
                        sessionPrefs.saveSessionId(newSession.sessionId)
                    } else {
                        Log.e("SessionViewModel", "Error al crear la nueva sesión")
                    }
                }
            }
        }
    }

    /**
     * Se invoca cuando el usuario acepta cerrar las sesiones activas en otros dispositivos.
     * Desactiva todas las sesiones activas (en la subcolección del usuario) y crea una nueva para el dispositivo actual.
     */
    fun closeSessionsAndCreateNew() {
        viewModelScope.launch {
            val currentUser = auth.currentUser ?: run {
                Log.e("SessionViewModel", "No hay usuario autenticado")
                return@launch
            }
            val uid = currentUser.uid
            Log.d("SessionViewModel", "Cerrando sesiones para UID: $uid")
            sessionRepository.deactivateSessions(uid)
            val deviceId = getDeviceIdUseCase()
            val newSession = sessionRepository.forceCreateNewSession(deviceId)
            if (newSession != null) {
                Log.d("SessionViewModel", "Nueva sesión creada tras forzar el cierre: ${newSession.sessionId}")
                _sessionState.value = newSession
                sessionPrefs.saveSessionId(newSession.sessionId)
            } else {
                Log.e("SessionViewModel", "Error al crear nueva sesión tras forzar cierre")
            }
        }
    }

    /**
     * Cierra la sesión: desactiva todas las sesiones activas en la subcolección del usuario y limpia el sessionId local.
     */
    fun closeSession() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            sessionRepository.deactivateSessions(uid)
            _sessionState.value = null
            sessionPrefs.clearSessionId()
            Log.d("SessionViewModel", "Sesión cerrada para UID: $uid")
        }
    }

    /**
     * Forza el logout del usuario en caso de que la sesión se vuelva inactiva.
     */
    fun forceLogout() {
        Log.d("SessionViewModel", "Forzando logout: la sesión se volvió inactiva.")
        // Aquí se debería implementar la lógica de logout forzado:
        // Por ejemplo, invocar auth.signOut() y notificar a la UI para navegar a la pantalla de login.
    }
}
