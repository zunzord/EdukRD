package com.edukrd.data.repository

import com.edukrd.data.model.Rating
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RatingRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val ratingsCollection = firestore.collection("ratings")

    /**
     * Crea o actualiza la valoración del usuario para un curso.
     * Limita a 3 envíos diarios.
     *
     * @param courseId ID del curso.
     * @param userId ID del usuario.
     * @param value Valoración (1 a 5).
     * @param feedback Comentario del usuario (opcional).
     * @param serverDate Fecha del servidor en formato "yyyy-MM-dd".
     *
     * Si ya existen 3 ratings para ese día, lanza una excepción.
     */
    suspend fun submitRating(
        courseId: String,
        userId: String,
        value: Int,
        feedback: String,
        serverDate: String
    ) {
        // Consulta cuántos ratings ya existen para este usuario, curso y fecha
        val querySnapshot = ratingsCollection
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("userId", userId)
            .whereEqualTo("serverDate", serverDate)
            .get()
            .await()

        if (querySnapshot.size() >= 3) {
            throw Exception("Solo puedes calificar este curso hasta 3 veces por día.")
        }

        // Generamos un ID único para permitir múltiples envíos en el mismo día.
        val docId = "${courseId}_${userId}_${System.currentTimeMillis()}"

        val data = Rating.toMap(userId, courseId, value, feedback, serverDate)
        ratingsCollection.document(docId).set(data).await()
    }

    /**
     * Devuelve el promedio global de rating para un curso.
     * Se toma únicamente el registro más reciente de cada usuario.
     */
    fun getAverageRating(courseId: String): Flow<Float> = callbackFlow {
        val listener = firestore.collection("ratings")
            .whereEqualTo("courseId", courseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else {
                    // Agrupar por userId y para cada grupo tomar el documento con el timestamp más reciente
                    val latestRatings = snapshot?.documents
                        ?.groupBy { it.getString("userId") }
                        ?.mapNotNull { (_, docs) ->
                            docs.maxByOrNull { doc ->
                                doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                            }?.getLong("value")
                        } ?: emptyList()
                    val avg = if (latestRatings.isNotEmpty()) latestRatings.average().toFloat() else 0f
                    trySend(avg)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Obtiene el último rating realizado por el usuario para un curso (o null si no hay).
     * Se ordena por timestamp descendente y se toma el primero.
     */
    fun getUserRating(courseId: String, userId: String): Flow<Int?> = callbackFlow {
        val listener = ratingsCollection
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else {
                    trySend(snapshot?.documents?.firstOrNull()?.getLong("value")?.toInt())
                }
            }
        awaitClose { listener.remove() }
    }
}
