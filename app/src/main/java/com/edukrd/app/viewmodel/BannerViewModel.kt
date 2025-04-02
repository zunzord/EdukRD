package com.edukrd.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BannerViewModel @Inject constructor(
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _imageUrls = MutableStateFlow<List<String>>(emptyList())
    val imageUrls: StateFlow<List<String>> get() = _imageUrls

    // Estado para el índice actual del carrusel
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> get() = _currentIndex

    init {
        viewModelScope.launch {
            val urls = imageRepository.getImageUrls()
            _imageUrls.value = urls
            Log.d("BannerViewModel", "URLs obtenidas: $urls")

            // Si hay imágenes, inicia el ciclo de cambio cada 5 segundos
            if (urls.isNotEmpty()) {
                while (true) {
                    delay(5000) // 5000 ms = 5 segundos
                    _currentIndex.value = (_currentIndex.value + 1) % urls.size
                }
            }
        }
    }
}
