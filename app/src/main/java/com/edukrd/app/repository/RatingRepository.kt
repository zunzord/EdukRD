package com.edukrd.data.repository

import com.edukrd.data.model.Rating
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class RatingRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val ratingsCollection = firestore.collection("ratings")

    /** Crea o actualiza la valoración del usuario para un curso */
    suspend fun submitRating(courseId: String, userId: String, value: Int) {
        val docId = "${courseId}_$userId"
        val data = Rating.toMap(userId, courseId, value)
        ratingsCollection.document(docId).set(data).await()
    }

    /** Devuelve el promedio de rating (1–5) para un curso */
    fun getAverageRating(courseId: String): Flow<Float> = callbackFlow {
        val listener = firestore.collection("ratings")
            .whereEqualTo("courseId", courseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else {
                    val values = snapshot?.documents?.mapNotNull { it.getLong("value") } ?: emptyList()
                    trySend(if (values.isNotEmpty()) values.average().toFloat() else 0f)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getUserRating(courseId: String, userId: String): Flow<Int?> = callbackFlow {
        val listener = ratingsCollection.document("${courseId}_$userId")
            .addSnapshotListener { snap, err ->
                if (err != null) close(err)
                else trySend(snap?.getLong("value")?.toInt())
            }
        awaitClose { listener.remove() }
    }

}
