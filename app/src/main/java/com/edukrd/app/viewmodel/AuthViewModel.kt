package com.edukrd.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Define un sellado para representar los posibles resultados de autenticación.
sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    // Canal para emitir resultados de autenticación.
    private val _authResult = Channel<AuthResult>(Channel.BUFFERED)
    val authResult = _authResult.receiveAsFlow()

    // Exposición del UID del usuario actual.
    private val _uid = MutableStateFlow(auth.currentUser?.uid)
    val uid: StateFlow<String?> = _uid

    /**
     * Realiza el login del usuario utilizando await para manejar el resultado de forma asíncrona.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _uid.value = auth.currentUser?.uid
                _authResult.trySend(AuthResult.Success)
            } catch (e: Exception) {
                _authResult.trySend(AuthResult.Error(e.message ?: "Error desconocido al iniciar sesión"))
            }
        }
    }

    /**
     * Registra un nuevo usuario. Se crea la cuenta en Firebase Auth; si la creación es exitosa,
     * se intenta enviar el correo de verificación. Cualquier error durante el envío del correo se captura
     * y se comunica mediante AuthResult.Error.
     */
    fun register(email: String, password: String) {
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    _uid.value = user.uid
                    // Intenta enviar el correo de verificación.
                    try {
                        user.sendEmailVerification().await()
                        _authResult.trySend(AuthResult.Success)
                    } catch (ex: Exception) {
                        _authResult.trySend(AuthResult.Error("Error al enviar el correo de verificación: ${ex.message}"))
                    }
                } else {
                    _authResult.trySend(AuthResult.Error("Error: Usuario nulo tras registro"))
                }
            } catch (e: Exception) {
                _authResult.trySend(AuthResult.Error(e.message ?: "Error desconocido al registrar usuario"))
            }
        }
    }

    /**
     * Envía un correo de recuperación de contraseña utilizando await para gestionar la tarea.
     */
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _authResult.trySend(AuthResult.Success)
            } catch (e: Exception) {
                _authResult.trySend(AuthResult.Error(e.message ?: "Error desconocido al enviar correo de recuperación"))
            }
        }
    }
}
