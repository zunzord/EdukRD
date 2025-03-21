package com.edukrd.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.utils.isInternetAvailable
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerificationPendingViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    sealed class ResendState {
        object Idle : ResendState()
        object Loading : ResendState()
        data class Success(val message: String) : ResendState()
        data class Error(val message: String) : ResendState()
    }

    private val _resendState = MutableStateFlow<ResendState>(ResendState.Idle)
    val resendState: StateFlow<ResendState> = _resendState

    fun resendVerificationEmail(context: Context) {
        viewModelScope.launch {
            if (!isInternetAvailable(context)) {
                _resendState.value = ResendState.Error("Sin conexión a Internet")
                return@launch
            }
            val user = auth.currentUser
            if (user == null) {
                _resendState.value = ResendState.Error("No se encontró usuario")
                return@launch
            }
            _resendState.value = ResendState.Loading
            user.sendEmailVerification().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _resendState.value = ResendState.Success("Correo reenviado con éxito")
                } else {
                    _resendState.value = ResendState.Error("El correo anterior fue enviado correctamente. Recuerda revizar en la bandeja de spam, o si estas verificando la cuenta de correo correcta. Si después de validar, sigues sin encontrar el correo, espera 1 minuto y pulsa reenviar nuevamente.")
                }
            }
        }
    }
}
