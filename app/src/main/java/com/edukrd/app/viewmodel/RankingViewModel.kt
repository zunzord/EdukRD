package com.edukrd.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    // Exposición del ranking: cada entrada es Triple<userId, name, coins>
    private val _ranking = MutableStateFlow<List<Triple<String, String, Int>>>(emptyList())
    val ranking: StateFlow<List<Triple<String, String, Int>>> = _ranking

    // Posición y monedas del usuario actual
    private val _currentUserRank = MutableStateFlow<Int?>(null)
    val currentUserRank: StateFlow<Int?> = _currentUserRank

    private val _currentUserCoins = MutableStateFlow(0)
    val currentUserCoins: StateFlow<Int> = _currentUserCoins

    // Estados de carga y error
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Carga el ranking de usuarios ordenado por "coins" en orden descendente.
     * También determina la posición y monedas del usuario actual.
     */
    fun loadRanking() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val usersSnapshot = firestore.collection("users")
                    .orderBy("coins", Query.Direction.DESCENDING)
                    .limit(250)
                    .get()
                    .await()

                val rankedUsers = usersSnapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: "Usuario Anónimo"
                    val coins = doc.getLong("coins")?.toInt() ?: 0
                    Triple(doc.id, name, coins)
                }
                _ranking.value = rankedUsers

                // Determinar la posición y monedas del usuario actual
                val currentUserId = auth.currentUser?.uid
                rankedUsers.forEachIndexed { index, (userId, _, coins) ->
                    if (userId == currentUserId) {
                        _currentUserRank.value = index + 1
                        _currentUserCoins.value = coins
                    }
                }
            } catch (e: Exception) {
                Log.e("RankingViewModel", "Error al obtener ranking", e)
                _error.value = "Error al cargar el ranking"
            }

            _loading.value = false
        }
    }
}
