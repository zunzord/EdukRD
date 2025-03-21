package com.edukrd.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.models.User
import com.edukrd.app.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    sealed class NavigationCommand {
        object ToOnboarding : NavigationCommand()
        object ToHome : NavigationCommand()
        object ContactSupport : NavigationCommand()
    }

    private val TAG = "UserViewModel"

    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData

    private val _coins = MutableStateFlow(0)
    val coins: StateFlow<Int> = _coins

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _navigationCommand = Channel<NavigationCommand>(Channel.BUFFERED)
    val navigationCommand = _navigationCommand.receiveAsFlow()

    /**
     * Carga los datos del usuario actual y emite el comando de navegación apropiado.
     */
    fun loadCurrentUserData() {
        viewModelScope.launch {
            _loading.value = true
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                Log.e(TAG, "UID no disponible")
                _navigationCommand.trySend(NavigationCommand.ContactSupport)
                _loading.value = false
                return@launch
            }

            try {
                val data = userRepository.getUserData(uid)
                _userData.value = data
                _coins.value = data?.coins ?: 0

                when {
                    data == null -> _navigationCommand.trySend(NavigationCommand.ContactSupport)
                    data.primerAcceso -> _navigationCommand.trySend(NavigationCommand.ToOnboarding)
                    else -> _navigationCommand.trySend(NavigationCommand.ToHome)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando datos del usuario", e)
                _navigationCommand.trySend(NavigationCommand.ContactSupport)
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Marca primerAcceso = false luego de completar el onboarding.
     */
    fun markOnboardingCompleted(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            val currentUser = _userData.value
            if (currentUser == null) {
                Log.e(TAG, "Usuario no cargado para marcar onboarding")
                onResult(false)
                _loading.value = false
                return@launch
            }
            val updated = currentUser.copy(primerAcceso = false)
            updateCurrentUserData(updated) { success ->
                if (success) {
                    _userData.value = updated
                    _coins.value = updated.coins
                }
                onResult(success)
                _loading.value = false
            }
        }
    }

    /**
     * Crea o actualiza los datos del usuario en Firestore (merge) y actualiza el estado local.
     */
    fun updateCurrentUserData(updatedUser: User, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            val uid = withTimeoutOrNull(5000L) {
                while (auth.currentUser?.uid == null) delay(100)
                auth.currentUser?.uid
            }
            if (uid.isNullOrBlank()) {
                Log.e(TAG, "UID no obtenido para actualización")
                onResult(false)
                _loading.value = false
                return@launch
            }

            val updateMap = mapOf<String, Any?>(
                "name" to updatedUser.name,
                "lastName" to updatedUser.lastName,
                "birthDate" to updatedUser.birthDate,
                "sector" to updatedUser.sector,
                "phone" to updatedUser.phone,
                "email" to updatedUser.email,
                "notificationsEnabled" to updatedUser.notificationsEnabled,
                "notificationFrequency" to updatedUser.notificationFrequency,
                "themePreference" to updatedUser.themePreference,
                "coins" to updatedUser.coins,
                "primerAcceso" to updatedUser.primerAcceso,
                "createdAt" to updatedUser.createdAt
            )

            try {
                val success = userRepository.updateUserData(uid, updateMap)
                if (success) {
                    _userData.value = updatedUser
                    _coins.value = updatedUser.coins
                } else {
                    Log.e(TAG, "Fallo al actualizar datos para UID=$uid")
                }
                onResult(success)
            } catch (e: Exception) {
                Log.e(TAG, "Excepción en updateCurrentUserData", e)
                onResult(false)
            } finally {
                _loading.value = false
            }
        }
    }
}
