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

    // Canal para notificar a la UI cuando se detecta un conflicto de sesión (sesión activa en otro dispositivo).
    private val _sessionConflictEvent = Channel<String>(Channel.BUFFERED)
    val sessionConflictEvent = _sessionConflictEvent.receiveAsFlow()

    init {
        // Si en preferencias ya existe un sessionId, inicia un listener para esa sesión.
        viewModelScope.launch {
            sessionPrefs.getSessionId()?.let { storedSessionId ->
                Log.d("SessionViewModel", "Iniciando listener para sessionId: $storedSessionId")
                sessionRepository.getSessionFlow(storedSessionId).collect { session ->
                    _sessionState.value = session
                    Log.d("SessionViewModel", "Listener detectó sesión: ${session?.sessionId} activa: ${session?.active}")
                    // Si la sesión detectada se vuelve inactiva, se fuerza el logout.
                    if (session != null && !session.active) {
                        forceLogout()
                    }
                }
            }
        }
    }

    /**
     * Verifica si existe una sesión activa para el usuario actual y actúa en consecuencia:
     * - Si existe y corresponde al dispositivo actual (deviceId y stored sessionId coinciden), la usa.
     * - Si existe pero no es de este dispositivo, emite un evento de conflicto para que la UI lo maneje.
     * - Si no existe ninguna sesión activa, crea una nueva sesión.
     */
    fun createNewSession() {
        viewModelScope.launch {
            val currentUser = auth.currentUser ?: run {
                Log.e("SessionViewModel", "No hay usuario autenticado")
                return@launch
            }
            val uid = currentUser.uid
            val deviceId = getDeviceIdUseCase() // Obtiene el ID del dispositivo.
            Log.d("SessionViewModel", "Obtenido deviceId: $deviceId")

            // Consultamos si existe una sesión activa para este usuario.
            val activeSession = sessionRepository.getActiveSession(uid)
            val storedSessionId = sessionPrefs.getSessionId()

            when {
                activeSession != null -> {
                    if (storedSessionId != null &&
                        storedSessionId == activeSession.sessionId &&
                        activeSession.deviceId == deviceId) {
                        // La sesión activa corresponde al dispositivo actual.
                        Log.d("SessionViewModel", "Sesión activa existente en este dispositivo: ${activeSession.sessionId}")
                        _sessionState.value = activeSession
                    } else {
                        // Existe una sesión activa, pero no corresponde a este dispositivo.
                        Log.d("SessionViewModel", "Sesión activa en otro dispositivo detectada")
                        _sessionConflictEvent.trySend(
                            "Ya existe una sesión activa en otro dispositivo. ¿Deseas cerrar las sesiones previas y continuar?"
                        )
                    }
                }
                else -> {
                    // No existe sesión activa, se crea una nueva.
                    val newSession = sessionRepository.createSession(deviceId)
                    if (newSession != null) {
                        Log.d("SessionViewModel", "Nueva sesión creada: ${newSession.sessionId}")
                        _sessionState.value = newSession
                        sessionPrefs.saveSessionId(newSession.sessionId)
                    } else {
                        Log.e("SessionViewModel", "Error al crear nueva sesión")
                    }
                }
            }
        }
    }

    /**
     * Se invoca cuando el usuario acepta cerrar las sesiones activas en otros dispositivos.
     * Desactiva todas las sesiones activas y crea una nueva.
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
            val newSession = sessionRepository.createSession(deviceId)
            if (newSession != null) {
                Log.d("SessionViewModel", "Nueva sesión tras cierre: ${newSession.sessionId}")
                _sessionState.value = newSession
                sessionPrefs.saveSessionId(newSession.sessionId)
            } else {
                Log.e("SessionViewModel", "Error al crear nueva sesión tras cierre")
            }
        }
    }

    /**
     * Cierra (desactiva) todas las sesiones del usuario y limpia el estado de la sesión.
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
     * Función para forzar el logout si la sesión se vuelve inactiva.
     * Aquí puedes realizar la lógica necesaria (por ejemplo, llamar a FirebaseAuth.signOut())
     * y notificar a la UI para que navegue a la pantalla de login.
     */
    fun forceLogout() {
        Log.d("SessionViewModel", "Forzando logout porque la sesión se volvió inactiva.")
        // Ejemplo: FirebaseAuth.getInstance().signOut()
        // Y notificar a la UI mediante un callback o mediante otro canal/estado.
    }
}
