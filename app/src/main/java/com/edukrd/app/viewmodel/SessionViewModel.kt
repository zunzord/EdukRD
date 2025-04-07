package com.edukrd.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.data.preferences.SessionPreferences
import com.edukrd.app.models.Session
import com.edukrd.app.repository.SessionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val auth: FirebaseAuth,
    private val sessionPrefs: SessionPreferences // Inyección de preferencias de sesión
) : ViewModel() {

    // Estado de la sesión actual (nula si no hay sesión)
    private val _sessionState = MutableStateFlow<Session?>(null)
    val sessionState: StateFlow<Session?> = _sessionState.asStateFlow()

    init {
        // Inicia un listener (Flow) si el dispositivo ya tiene almacenado un sessionId.
        // Esto permitirá que si esa sesión se vuelve inactiva, se notifique para forzar el logout.
        viewModelScope.launch {
            sessionPrefs.getSessionId()?.let { storedSessionId ->
                sessionRepository.getSessionFlow(storedSessionId).collect { session ->
                    _sessionState.value = session
                    if (session != null && !session.active) {
                        // Si la sesión cambia a inactiva, notifica para forzar el logout o tomar la acción necesaria.
                        forceLogout()
                    }
                }
            }
        }
    }

    /**
     * Crea una nueva sesión para el usuario actual utilizando el deviceId proporcionado.
     * La lógica es la siguiente:
     * 1. Verificar si existe una sesión activa.
     * 2. Si existe y el sessionId coincide con el almacenado en preferencias (es decir, es la misma sesión),
     *    simplemente se usa esa sesión.
     * 3. Si existe pero el sessionId es distinto (lo que indica que hay otra sesión activa en otro dispositivo),
     *    se puede notificar al usuario que ya hay una sesión activa (y definir si se cierra la anterior o se solicita acción).
     * 4. Si no existe sesión activa, se crea una nueva y se almacena el sessionId.
     */
    fun createNewSession(deviceId: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch

            // Verifica si hay una sesión activa para este usuario.
            val activeSession = sessionRepository.getActiveSession(uid)
            val storedSessionId = sessionPrefs.getSessionId()
            if (activeSession != null) {
                if (storedSessionId != null && storedSessionId == activeSession.sessionId) {
                    // La sesión activa corresponde al dispositivo actual.
                    _sessionState.value = activeSession
                } else {
                    // Existe otra sesión activa (en otro dispositivo).
                    // Aquí puedes notificar al usuario, por ejemplo, enviando un mensaje de error o pidiendo confirmación.
                    // Para este ejemplo, se desactivarán todas las sesiones activas y se creará una nueva.
                    sessionRepository.deactivateSessions(uid)
                    val newSession = sessionRepository.createSession(deviceId)
                    if (newSession != null) {
                        _sessionState.value = newSession
                        sessionPrefs.saveSessionId(newSession.sessionId)
                    }
                }
            } else {
                // No hay sesión activa, crea una nueva.
                val newSession = sessionRepository.createSession(deviceId)
                if (newSession != null) {
                    _sessionState.value = newSession
                    sessionPrefs.saveSessionId(newSession.sessionId)
                }
            }
        }
    }

    /**
     * Cierra la sesión actual del usuario.
     */
    fun closeSession() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            sessionRepository.deactivateSessions(uid)
            _sessionState.value = null
            sessionPrefs.clearSessionId()
        }
    }

    /**
     * Función para forzar el logout.
     * Por ejemplo, se llama cuando el listener detecta que la sesión activa ha cambiado a inactiva.
     */
    fun forceLogout() {
        // Aquí se puede llamar a FirebaseAuth.signOut() y realizar otras acciones necesarias para forzar el logout.
        // NOTA: Esta función debe coordinar con el flujo de la aplicación para navegar a la pantalla de login.
    }
}
