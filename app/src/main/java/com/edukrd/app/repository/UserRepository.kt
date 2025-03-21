package com.edukrd.app.repository

import android.util.Log
import com.edukrd.app.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val TAG = "UserRepository"

    /**
     * Obtiene los datos del usuario desde Firestore.
     */
    suspend fun getUserData(userId: String): User? {
        Log.d(TAG, "Iniciando obtención de datos para usuario: $userId")
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                Log.d(TAG, "Documento encontrado. Datos: ${document.data}")
                document.toObject(User::class.java)
            } else {
                Log.e(TAG, "Documento no existe para UID: $userId")
                null
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore error [${e.code}]: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado en getUserData()", e)
            null
        }
    }

    /**
     * Crea o actualiza los datos del usuario en Firestore usando merge.
     */
    suspend fun updateUserData(userId: String, userData: Map<String, Any?>): Boolean {
        Log.d(TAG, "Iniciando actualización para usuario: $userId — Campos: ${userData.keys}")
        return try {
            firestore.collection("users")
                .document(userId)
                .set(userData, SetOptions.merge())
                .await()

            Log.d(TAG, "Actualización exitosa para UID: $userId")
            true
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore error [${e.code}]: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado en updateUserData()", e)
            false
        }
    }
}
