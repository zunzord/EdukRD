package com.edukrd.app.repository

import com.edukrd.app.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
     * Crea o actualiza los datos del usuario en Firestore.
     * Si el documento no existe, lo creará. Si ya existe, lo sobreescribirá
     * o hará merge si lo especificas con SetOptions.merge().
     *
     * @param userId ID del usuario (UID de FirebaseAuth).
     * @param userData Mapa con los campos a guardar/actualizar.
     * @return true si la operación fue exitosa, false en caso contrario.
     */
    suspend fun updateUserData(userId: String, userData: Map<String, Any>): Boolean {
        return try {
            firestore.collection("users")
                .document(userId)
                // .set(userData, SetOptions.merge()) // Si quisieras hacer merge en lugar de overwrite
                .set(userData) // Crea el doc si no existe; sobreescribe campos si existe
                .await()
            true
        } catch (e: Exception) {
            // Aquí podrías registrar el error si es necesario.
            false
        }
    }
}
