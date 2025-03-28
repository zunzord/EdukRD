package com.edukrd.app.repository

import com.edukrd.app.models.Course
import com.edukrd.app.usecase.GetServerDateUseCase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val getServerDateUseCase: GetServerDateUseCase
) {
    private val dailyLimit = 5

    /**
     * Calcula el rango del día (inicio y fin) basado en la fecha del servidor obtenida desde GetServerDateUseCase.
     * Esto permite que el conteo de transacciones se haga de acuerdo a la fecha oficial, no al reloj del dispositivo.
     */
    private suspend fun getServerDayRange(): Pair<Timestamp, Timestamp> {
        // Se obtiene la fecha del servidor (LocalDate)
        val serverLocalDate = getServerDateUseCase()  // Devuelve LocalDate
        val zone = ZoneId.systemDefault()
        // Inicio del día (00:00) para la fecha del servidor
        val startInstant = serverLocalDate.atStartOfDay(zone).toInstant()
        // Inicio del día siguiente
        val endInstant = serverLocalDate.plusDays(1).atStartOfDay(zone).toInstant()
        return Timestamp(Date.from(startInstant)) to Timestamp(Date.from(endInstant))
    }

    /**
     * Determina cuántas monedas otorgar según:
     *  • Primera aprobación EVER → recompenza
     *  • Aprobaciones posteriores, hasta dailyLimit por día → recompenzaExtra
     *  • Si excede dailyLimit hoy → 0
     *
     * Se usan las fechas del servidor para calcular el rango del día.
     */
    suspend fun awardCoinsForCourse(userId: String, course: Course): Int {
        // 1. Verificar si el usuario ya aprobó este curso alguna vez
        val everSnapshot = firestore.collection("examResults")
            .whereEqualTo("userId", userId)
            .whereEqualTo("courseId", course.id)
            .whereEqualTo("passed", true)
            .get()
            .await()

        if (everSnapshot.isEmpty) {
            return course.recompenza ?: 0
        }

        // 2. Contar las aprobaciones del día (usando la fecha del servidor)
        val (startOfDay, endOfDay) = getServerDayRange()
        val todaySnapshot = firestore.collection("examResults")
            .whereEqualTo("userId", userId)
            .whereEqualTo("courseId", course.id)
            .whereEqualTo("passed", true)
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThan("date", endOfDay)
            .get()
            .await()

        return if (todaySnapshot.size() < dailyLimit) course.recompenzaExtra ?: 0 else 0
    }

    /**
     * Actualiza las monedas del usuario incrementando el valor actual.
     */
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

    /**
     * Registra una transacción de monedas (por ejemplo, por aprobar un examen o canjear monedas).
     * Se utiliza FieldValue.serverTimestamp() para que el servidor asigne la fecha y hora exacta de la transacción.
     */
    suspend fun logCoinTransaction(userId: String, course: Course, coinsAwarded: Int): Boolean {
        return try {
            val data = mapOf(
                "userId" to userId,
                "courseId" to course.id,
                "coinsAwarded" to coinsAwarded,
                "timestamp" to FieldValue.serverTimestamp()
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
