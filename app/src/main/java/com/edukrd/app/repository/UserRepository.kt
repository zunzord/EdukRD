package com.edukrd.app.repository

import com.edukrd.app.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /**
     * Obtiene los datos del usuario desde Firestore.
     */
    suspend fun getUserData(userId: String): User? {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            if (document.exists()) {
                document.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            // Aquí podrías registrar el error si es necesario.
            null
        }
    }

    /**
     * Actualiza los datos del usuario en Firestore.
     * Recibe un mapa con los datos a actualizar.
     */
    suspend fun updateUserData(userId: String, userData: Map<String, Any>): Boolean {
        return try {
            firestore.collection("users").document(userId).update(userData).await()
            true
        } catch (e: Exception) {
            // Aquí podrías registrar el error si es necesario.
            false
        }
    }
}
