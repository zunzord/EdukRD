package com.edukrd.app.repository

import com.edukrd.app.models.Course
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Límite diario de aprobaciones para otorgar monedas por curso
    private val dailyLimit = 5

    /**
     * Obtiene el rango [start, end) del día actual (00:00 de hoy a 00:00 de mañana).
     */
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
        return Pair(start, end)
    }

    /**
     * Determina cuántas monedas otorgar para un curso,
     * basándose en si es la primera vez que se aprueba hoy
     * o si ya se aprobó antes pero no se alcanzó el límite diario.
     *
     * @param userId ID del usuario.
     * @param course Instancia de Course, con campos recompensa y recompensaExtra.
     * @return Cantidad de monedas a otorgar (0 si se alcanzó el límite).
     */
    suspend fun awardCoinsForCourse(userId: String, course: Course): Int {
        val (startOfDay, endOfDay) = getDayRange()

        // Query a examResults aprobados hoy para este usuario y curso
        val snapshot = firestore.collection("examResults")
            .whereEqualTo("userId", userId)
            .whereEqualTo("courseId", course.id)
            .whereEqualTo("passed", true)
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThan("date", endOfDay)
            .get()
            .await()

        val timesApprovedToday = snapshot.size()

        return when {
            // Primera vez hoy
            timesApprovedToday == 0 -> course.recompenza ?: 0
            // Subsecuentes, pero sin exceder el dailyLimit
            timesApprovedToday in 1 until dailyLimit -> course.recompenzaExtra ?: 0
            // Si se supera el límite diario
            else -> 0
        }
    }

    /**
     * Actualiza el saldo de monedas del usuario en Firestore sumando coinsToAdd.
     *
     * @param userId ID del usuario.
     * @param coinsToAdd Cantidad de monedas a sumar al saldo actual.
     * @return true si la operación fue exitosa, false en caso contrario.
     */
    suspend fun updateUserCoins(userId: String, coinsToAdd: Int): Boolean {
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(userId)
                val snapshot = transaction.get(userRef)
                val currentCoins = snapshot.getLong("coins")?.toInt() ?: 0
                val newCoins = currentCoins + coinsToAdd
                transaction.update(userRef, "coins", newCoins)
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Registra una transacción de monedas en la colección "coinTransactions",
     * para llevar un histórico de las entregas de monedas.
     *
     * @param userId ID del usuario.
     * @param course Curso asociado.
     * @param coinsAwarded Cantidad de monedas entregadas.
     * @return true si se registró correctamente, false en caso de error.
     */
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
