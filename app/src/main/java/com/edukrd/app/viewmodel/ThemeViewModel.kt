package com.edukrd.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _themePreference = MutableStateFlow("light")
    val themePreference: StateFlow<String> = _themePreference

    init {
        loadThemePreference()
    }

    /**
     * Carga la preferencia de tema del usuario actual desde Firestore.
     */
    fun loadThemePreference() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    val document = firestore.collection("users").document(uid).get().await()
                    _themePreference.value = document.getString("themePreference") ?: "light"
                } catch (e: Exception) {
                    _themePreference.value = "light"
                }
            }
        }
    }

    /**
     * Actualiza la preferencia de tema de forma local y persiste el cambio en Firestore.
     * Esta función actualiza el StateFlow inmediatamente, lo que debe provocar la recomposición de MainActivity.
     */
    fun updateThemePreference(newTheme: String) {
        _themePreference.value = newTheme
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    firestore.collection("users").document(uid)
                        .update("themePreference", newTheme)
                        .await()
                } catch (e: Exception) {
                    // Opcional: registrar error si es necesario.
                }
            }
        }
    }
}
