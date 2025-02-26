package com.edukrd.app.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    // Obtener datos del usuario
    suspend fun getUserData(userId: String): Map<String, Any>? {
        return try {
            val document = db.collection("users").document(userId).get().await()
            if (document.exists()) {
                document.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Obtener cursos
    suspend fun getCourses(): List<com.edukrd.app.models.Course> {
        return try {
            val snapshot = db.collection("courses").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.edukrd.app.models.Course::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Actualizar progreso del usuario en un curso
    suspend fun updateUserProgress(progress: com.edukrd.app.models.UserProgress) {
        db.collection("progress").document("${progress.userId}_${progress.courseId}")
            .set(progress)
            .await()
    }
}
