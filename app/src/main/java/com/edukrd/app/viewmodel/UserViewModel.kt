package com.edukrd.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.models.User
import com.edukrd.app.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

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
                // No hay usuario autenticado
                _userData.value = null
                _coins.value = 0
                _loading.value = false
                return@launch
            }
            try {
                val data = userRepository.getUserData(uid)
                _userData.value = data
                _coins.value = data?.coins ?: 0
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error al cargar datos del usuario", e)
                _userData.value = null
                _coins.value = 0
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Actualiza los datos del usuario actual en Firestore.
     * Si el UID no está disponible de inmediato (por ejemplo, justo después del registro),
     * espera hasta 5 segundos para obtenerlo.
     *
     * @param updatedUser: Usuario con los datos actualizados.
     * @param onResult: Callback que indica el resultado de la operación (true/false).
     */
    fun updateCurrentUserData(updatedUser: User, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true

            // Espera hasta que auth.currentUser esté disponible (máximo 5 segundos)
            val uid = withTimeoutOrNull(5000L) {
                while (auth.currentUser?.uid == null) {
                    delay(100)
                }
                auth.currentUser?.uid
            }
            if (uid == null) {
                onResult(false)
                _loading.value = false
                return@launch
            }

            // Agregamos el campo createdAt para registrar la fecha de creación
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
                "createdAt" to (updatedUser.createdAt as Any)
            )

            try {
                val success = userRepository.updateUserData(uid, updateMap)
                if (success) {
                    // Actualiza el estado local si la operación fue exitosa
                    _userData.value = updatedUser
                    _coins.value = updatedUser.coins
                }
                onResult(success)
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error al actualizar datos del usuario", e)
                onResult(false)
            } finally {
                _loading.value = false
            }
        }
    }
}
