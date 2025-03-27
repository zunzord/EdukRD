package com.edukrd.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.data.preferences.SessionPreferences
import com.edukrd.app.usecase.GetServerDateUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getServerDateUseCase: GetServerDateUseCase,
    private val sessionPrefs: SessionPreferences,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "SplashViewModel"

    fun checkSession(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // 1️⃣ Obtener offset bruto
                val offset = getServerDateUseCase.invokeOffset()
                Log.d(TAG, "Server offset (ms) = $offset")

                // 2️⃣ Obtener fecha convertida
                val now = getServerDateUseCase()
                Log.d(TAG, "Server date = $now")

                val lastLogin = sessionPrefs.getLoginDate()
                if (lastLogin == null) {
                    Log.d(TAG, "No last login date → navigate to Login")
                    onResult(false)
                    return@launch
                }

                val diff = ChronoUnit.MINUTES.between(lastLogin, now)
                Log.d(TAG, "Minutes since last login = $diff")

                if (diff > 15) {
                    auth.signOut()
                    onResult(false)
                } else {
                    onResult(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed fetching server time", e)
                auth.signOut()
                onResult(false)
            }
        }
    }
}
