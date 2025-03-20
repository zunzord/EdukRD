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
            Log.d(TAG, "Consultando Firestore en colección 'users' documento '$userId'")
            val document = firestore.collection("users").document(userId).get().await()

            if (document.exists()) {
                Log.d(TAG, "Documento encontrado. Campos disponibles: ${document.data?.keys}")
                Log.d(TAG, "Intentando conversión a objeto User...")

                val user = document.toObject(User::class.java)
                if (user == null) {
                    Log.e(TAG, "Fallo en conversión. Datos crudos del documento:")
                    Log.e(TAG, "${document.data}")
                    Log.e(TAG, "Campos esperados en clase User: ${User::class.java.declaredFields.joinToString { it.name }}")
                } else {
                    Log.d(TAG, "Conversión exitosa: $user")
                }
                user
            } else {
                Log.e(TAG, "Documento no existe en Firestore")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en getUserData()", e)
            when (e) {
                is FirebaseFirestoreException -> {
                    Log.e(TAG, "Error de Firestore [${e.code}]: ${e.message}")
                    Log.e(TAG, "¿Problema de permisos? ${e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED}")
                }
            }
            null
        }
    }

    /**
     * Crea o actualiza los datos del usuario en Firestore.
     */
    suspend fun updateUserData(userId: String, userData: Map<String, Any>): Boolean {
        Log.d(TAG, "Iniciando actualización para usuario: $userId")
        Log.d(TAG, "Datos a actualizar: ${userData.keys}")

        return try {
            firestore.collection("users")
                .document(userId)
                .set(userData, SetOptions.merge())
                .await()

            Log.d(TAG, "Actualización exitosa")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error en updateUserData()", e)
            when (e) {
                is FirebaseFirestoreException -> {
                    Log.e(TAG, "Error de Firestore [${e.code}]: ${e.message}")
                }
            }
            false
        }
    }
}