package com.edukrd.app.util

class ErrorMapper {
    fun map(exception: Exception): String {
        return when (exception) {
            is com.google.firebase.auth.FirebaseAuthException ->
                "Error de autenticación. Verifica tus credenciales."
            is com.google.firebase.firestore.FirebaseFirestoreException ->
                "Error en la base de datos. Inténtalo más tarde."
            is java.io.IOException ->
                "Error de red. Revisa tu conexión a Internet."
            else ->
                "Ha ocurrido un error inesperado: ${exception.localizedMessage ?: "desconocido"}"
        }
    }
}
