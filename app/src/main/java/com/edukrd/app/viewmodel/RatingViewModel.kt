package com.edukrd.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.edukrd.data.repository.RatingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

@HiltViewModel
class RatingViewModel @Inject constructor(
    private val repository: RatingRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _avgRating = MutableStateFlow(0f)
    val avgRating: StateFlow<Float> = _avgRating

    private val _userRating = MutableStateFlow<Int?>(null)
    val userRating: StateFlow<Int?> = _userRating

    fun loadRatings(courseId: String) {
        repository.getAverageRating(courseId)
            .onEach { _avgRating.value = it }
            .catch { /* Log error */ }
            .launchIn(viewModelScope)

        auth.currentUser?.uid?.let { uid ->
            repository.getUserRating(courseId, uid)
                .onEach { _userRating.value = it }
                .catch { /* Log error */ }
                .launchIn(viewModelScope)
        }
    }

    fun submitRating(courseId: String, value: Int) {
        auth.currentUser?.uid?.let { uid ->
            viewModelScope.launch {
                repository.submitRating(courseId, uid, value)
            }
        }
    }
}
