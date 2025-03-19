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
     * Realiza el login del usuario.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _authResult.trySend(AuthResult.Success)
                        // Actualiza el UID cuando se inicia sesión correctamente.
                        _uid.value = auth.currentUser?.uid
                    } else {
                        _authResult.trySend(
                            AuthResult.Error(task.exception?.message ?: "Error desconocido al iniciar sesión")
                        )
                    }
                }
        }
    }

    /**
     * Registra un nuevo usuario.
     */
    fun register(email: String, password: String) {
        viewModelScope.launch {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _authResult.trySend(AuthResult.Success)
                        // Actualiza el UID luego del registro.
                        _uid.value = auth.currentUser?.uid
                    } else {
                        _authResult.trySend(
                            AuthResult.Error(task.exception?.message ?: "Error desconocido al registrar usuario")
                        )
                    }
                }
        }
    }

    /**
     * Envía un correo de recuperación de contraseña.
     */
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _authResult.trySend(AuthResult.Success)
                    } else {
                        _authResult.trySend(
                            AuthResult.Error(task.exception?.message ?: "Error desconocido al enviar correo de recuperación")
                        )
                    }
                }
        }
    }
}
