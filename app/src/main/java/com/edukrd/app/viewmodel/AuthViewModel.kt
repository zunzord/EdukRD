package com.edukrd.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.data.preferences.SessionPreferences
import com.edukrd.app.usecase.GetServerDateUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val getServerDateUseCase: GetServerDateUseCase,
    private val sessionPrefs: SessionPreferences
) : ViewModel() {

    private val _authResult = Channel<AuthResult>(Channel.BUFFERED)
    val authResult = _authResult.receiveAsFlow()

    private val _uid = MutableStateFlow(auth.currentUser?.uid)
    val uid: StateFlow<String?> = _uid

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val serverDate = getServerDateUseCase()
                sessionPrefs.saveLoginDate(serverDate)
                _uid.value = auth.currentUser?.uid
                _authResult.trySend(AuthResult.Success)
            } catch (e: Exception) {
                _authResult.trySend(AuthResult.Error(e.message ?: "Error desconocido al iniciar sesión"))
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Usuario nulo tras registro")
                user.sendEmailVerification().await()
                val serverDate = getServerDateUseCase()
                sessionPrefs.saveLoginDate(serverDate)
                _uid.value = user.uid
                _authResult.trySend(AuthResult.Success)
            } catch (e: Exception) {
                _authResult.trySend(AuthResult.Error(e.message ?: "Error desconocido al registrar usuario"))
            }
        }
    }

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

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            // Reinicia cualquier estado según sea necesario.
        }
    }
}
