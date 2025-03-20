package com.edukrd.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.models.User
import com.edukrd.app.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "UserViewModel"

    // Estado para los datos del usuario
    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData

    // Estado para el saldo de monedas
    private val _coins = MutableStateFlow(0)
    val coins: StateFlow<Int> = _coins

    // Estado para el loading
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /**
     * Carga los datos del usuario actual (basado en auth.currentUser?.uid) y actualiza el saldo de monedas.
     */
    fun loadCurrentUserData() {
        viewModelScope.launch {
            _loading.value = true
            val uid = auth.currentUser?.uid
            if (uid == null) {
                Log.e(TAG, "No hay usuario autenticado")
                _userData.value = null
                _coins.value = 0
                _loading.value = false
                return@launch
            }
            try {
                val data = userRepository.getUserData(uid)
                if (data != null) {
                    Log.d(TAG, "Datos del usuario cargados: $data")
                    _userData.value = data
                    _coins.value = data.coins
                } else {
                    Log.e(TAG, "No se encontraron datos para el usuario con UID: $uid")
                    _userData.value = null
                    _coins.value = 0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar datos del usuario", e)
                _userData.value = null
                _coins.value = 0
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Actualiza los datos del usuario actual en Firestore.
     * Si el UID no está disponible de inmediato, espera hasta 5 segundos para obtenerlo.
     *
     * @param updatedUser: Usuario con los datos actualizados.
     * @param onResult: Callback que indica el resultado de la operación (true/false).
     */
    fun updateCurrentUserData(updatedUser: User, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true

            val uid = withTimeoutOrNull(5000L) {
                while (auth.currentUser?.uid == null) {
                    delay(100)
                }
                auth.currentUser?.uid
            }
            if (uid == null) {
                Log.e(TAG, "No se obtuvo UID en el tiempo esperado")
                onResult(false)
                _loading.value = false
                return@launch
            }

            val updateMap = mapOf(
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
                "createdAt" to (updatedUser.createdAt ?: com.google.firebase.Timestamp.now())
            )

            try {
                // Ejecutamos la actualización en el contexto IO para evitar bloquear la UI
                val success = withContext(Dispatchers.IO) {
                    userRepository.updateUserData(uid, updateMap)
                }
                if (success) {
                    Log.d(TAG, "Datos del usuario actualizados exitosamente")
                    _userData.value = updatedUser
                    _coins.value = updatedUser.coins
                } else {
                    Log.e(TAG, "Error en la actualización de datos para UID: $uid")
                }
                onResult(success)
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al actualizar datos del usuario", e)
                onResult(false)
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Actualiza el campo 'primerAcceso' del usuario actual en Firestore a false,
     * indicando que el onboarding ya fue completado.
     */
    fun markOnboardingCompleted(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val currentUser = _userData.value
            if (currentUser == null) {
                Log.e(TAG, "No hay datos del usuario para marcar onboarding completado")
                onResult(false)
                return@launch
            }
            val updatedUser = currentUser.copy(primerAcceso = false)
            updateCurrentUserData(updatedUser) { success ->
                if (success) {
                    Log.d(TAG, "Onboarding marcado como completado")
                    _userData.value = updatedUser
                }
                onResult(success)
            }
        }
    }
}
