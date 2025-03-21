package com.edukrd.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "OnboardingViewModel"
    private val _completeChannel = Channel<Boolean>(Channel.BUFFERED)
    val complete = _completeChannel.receiveAsFlow()

    /**
     * Marca primerAcceso=false en Firestore directamente usando el repositorio.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                Log.e(TAG, "No hay usuario autenticado")
                _completeChannel.trySend(false)
                return@launch
            }

            val success = userRepository.updateUserData(uid, mapOf("primerAcceso" to false))
            Log.d(TAG, if (success) "Onboarding completado" else "Error completando onboarding")
            _completeChannel.trySend(success)
        }
    }
}
