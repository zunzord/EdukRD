package com.edukrd.app.util

/**
 * Clase sellada que representa el estado del resultado de una operación asíncrona.
 */
sealed class ResultState<out T> {
    object Loading : ResultState<Nothing>()
    data class Success<out T>(val data: T) : ResultState<T>()
    data class Error(val errorMessage: String) : ResultState<Nothing>()
}
