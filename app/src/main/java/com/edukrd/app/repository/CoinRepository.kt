package com.edukrd.app.repository

import com.edukrd.app.models.Course
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val dailyLimit = 5

    private fun getDayRange(): Pair<Timestamp, Timestamp> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = Timestamp(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val end = Timestamp(calendar.time)
        return start to end
    }

    /**
     * Determina cuántas monedas otorgar según:
     *  • Primera aprobación EVER → recompenza
     *  • Aprobaciones posteriores, hasta dailyLimit por día → recompenzaExtra
     *  • Si excede dailyLimit hoy → 0
     */
    suspend fun awardCoinsForCourse(userId: String, course: Course): Int {
        // 1️⃣ ¿Ya aprobó este curso alguna vez?
        val everSnapshot = firestore.collection("examResults")
            .whereEqualTo("userId", userId)
            .whereEqualTo("courseId", course.id)
            .whereEqualTo("passed", true)
            .get()
            .await()

        // Primera vez EVER
        if (everSnapshot.isEmpty) {
            return course.recompenza ?: 0
        }

        // 2️⃣ Si ya aprobó antes, contamos aprobaciones de hoy
        val (startOfDay, endOfDay) = getDayRange()
        val todaySnapshot = firestore.collection("examResults")
            .whereEqualTo("userId", userId)
            .whereEqualTo("courseId", course.id)
            .whereEqualTo("passed", true)
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThan("date", endOfDay)
            .get()
            .await()

        return when {
            todaySnapshot.size() < dailyLimit -> course.recompenzaExtra ?: 0
            else -> 0
        }
    }

    suspend fun updateUserCoins(userId: String, coinsToAdd: Int): Boolean {
        return try {
            firestore.runTransaction { tx ->
                val userRef = firestore.collection("users").document(userId)
                val current = (tx.get(userRef).getLong("coins") ?: 0L).toInt()
                tx.update(userRef, "coins", current + coinsToAdd)
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun logCoinTransaction(userId: String, course: Course, coinsAwarded: Int): Boolean {
        return try {
            val data = mapOf(
                "userId" to userId,
                "courseId" to course.id,
                "coinsAwarded" to coinsAwarded,
                "timestamp" to Timestamp.now()
            )
            firestore.collection("coinTransactions")
                .add(data)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
