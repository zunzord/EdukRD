package com.edukrd.app.viewmodel

import android.util.Log
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

/**
 * Data class para representar la información de la medalla
 */
data class MedalData(
    val courseId: String,
    val title: String,
    val imageUrl: String
)

@HiltViewModel
class MedalViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _medals = MutableStateFlow<List<MedalData>>(emptyList())
    val medals: StateFlow<List<MedalData>> = _medals

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Carga las medallas del usuario actual (obtiene el UID internamente).
     */
    fun loadMedals() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val userId = auth.currentUser?.uid
            if (userId == null) {
                _error.value = "Usuario no autenticado"
                _loading.value = false
                return@launch
            }

            try {
                // 1. Obtener examResults aprobados
                val examResultsSnapshot = firestore.collection("examResults")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("passed", true)
                    .get()
                    .await()

                val passedCourseIds = examResultsSnapshot.documents.mapNotNull {
                    it.getString("courseId")
                }.toSet()

                if (passedCourseIds.isEmpty()) {
                    _medals.value = emptyList()
                    _loading.value = false
                    return@launch
                }

                // 2. Obtener la info de cada curso (título y medalla)
                val tempMedals = mutableListOf<MedalData>()
                for (courseId in passedCourseIds) {
                    val courseDoc = firestore.collection("courses")
                        .document(courseId)
                        .get()
                        .await()

                    if (courseDoc.exists()) {
                        val title = courseDoc.getString("title") ?: "Curso sin título"
                        val medallaUrl = courseDoc.getString("medalla") ?: ""
                        if (medallaUrl.isNotEmpty()) {
                            tempMedals.add(
                                MedalData(
                                    courseId = courseId,
                                    title = title,
                                    imageUrl = medallaUrl
                                )
                            )
                        }
                    }
                }

                _medals.value = tempMedals
            } catch (e: Exception) {
                Log.e("MedalViewModel", "Error al obtener medallas", e)
                _error.value = "Error al obtener medallas: ${e.message}"
            }

            _loading.value = false
        }
    }
}
