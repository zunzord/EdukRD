package com.edukrd.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.models.StoreItem
import com.edukrd.app.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val storeRepository: StoreRepository
) : ViewModel() {

    private val _availableItems = MutableStateFlow<List<StoreItem>>(emptyList())
    val availableItems: StateFlow<List<StoreItem>> = _availableItems

    private val _redeemedItems = MutableStateFlow<List<StoreItem>>(emptyList())
    val redeemedItems: StateFlow<List<StoreItem>> = _redeemedItems

    private val _receivedItems = MutableStateFlow<List<StoreItem>>(emptyList())
    val receivedItems: StateFlow<List<StoreItem>> = _receivedItems

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Estado para el resultado del canje: Triple(éxito, mensaje, artículo actualizado)
    private val _redeemResult = MutableStateFlow<Triple<Boolean, String, StoreItem?>?>(null)
    val redeemResult: StateFlow<Triple<Boolean, String, StoreItem?>?> = _redeemResult

    fun resetRedeemResult() {
        _redeemResult.value = null
    }

    /**
     * Carga la lista de artículos disponibles en la tienda.
     */


    fun loadAvailableItems() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _availableItems.value = storeRepository.getAvailableItems()
            } catch (e: Exception) {
                _error.value = e.message
            }
            _loading.value = false
        }
    }

    /**
     * Carga la lista de artículos canjeados por el usuario actual.
     * El repositorio se encarga de obtener el UID internamente, por lo que no necesitamos pasarlo aquí.
     */
    fun loadRedeemedItems() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _redeemedItems.value = storeRepository.getRedeemedItems()
            } catch (e: Exception) {
                _error.value = e.message
            }
            _loading.value = false
        }
    }

    /**
     * Carga la lista de artículos recibidos por el usuario actual.
     * De igual forma, no pasamos el userId, el repositorio obtiene el UID internamente.
     */
    fun loadReceivedItems() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _receivedItems.value = storeRepository.getReceivedItems()
            } catch (e: Exception) {
                _error.value = e.message
            }
            _loading.value = false
        }
    }

    /**
     * Realiza la transacción de canje de un artículo para el usuario actual.
     * Se utiliza el método `redeemItemTransaction` del StoreRepository, que realiza la transacción atómica:
     * - Verifica y decrementa el stock.
     * - Descuenta las monedas del usuario.
     * - Registra el canje en la subcolección "redeemed" del usuario y en la subcolección "redeemed" del artículo,
     *   generando un transactionId.
     *
     * Actualiza localmente la lista de artículos disponibles si el canje es exitoso.
     */
    fun redeemItem(item: StoreItem) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // Ahora el repositorio obtiene internamente el UID.
                val result = storeRepository.redeemItemTransaction(item)
                _redeemResult.value = result
                if (result.first && result.third != null) {
                    // Actualiza localmente la lista de artículos disponibles
                    _availableItems.value = _availableItems.value.map {
                        if (it.id == item.id) result.third!! else it
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _redeemResult.value = Triple(false, e.message ?: "Error desconocido", null)
            }
            _loading.value = false
        }
    }
}
