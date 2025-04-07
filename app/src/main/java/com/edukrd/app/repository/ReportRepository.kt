package com.edukrd.app.repository

import com.edukrd.app.models.Report
import com.edukrd.app.usecase.GetServerDateUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class ReportRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val getServerDateUseCase: GetServerDateUseCase
) {

    // Referencia a la colección "reports" en la raíz de Firestore.
    private val reportsCollection = firestore.collection("reports")

    /**
     * Retorna el número de reportes que el usuario ha enviado en el día actual.
     */
    suspend fun getReportsCountForToday(userId: String): Int {
        val today: LocalDate = getServerDateUseCase()
        val zoneId = ZoneId.systemDefault()
        // Definir el inicio y fin del día actual.
        val startOfDay = today.atStartOfDay(zoneId).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant()

        return try {
            val querySnapshot = reportsCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("createdAt", com.google.firebase.Timestamp(java.util.Date.from(startOfDay)))
                .whereLessThan("createdAt", com.google.firebase.Timestamp(java.util.Date.from(endOfDay)))
                .get()
                .await()
            querySnapshot.size()
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Envía un reporte (sugerencia o incidencia).
     * - Verifica que el usuario no haya excedido el límite de 20 reportes por día.
     * - Genera una nueva referencia de documento para asignar el reportId de forma atómica.
     * - Completa los campos restantes del reporte (email, phone, sector, etc.) internamente.
     *
     * Devuelve true si se crea el reporte exitosamente, o false en caso de error o límite excedido.
     */
    suspend fun submitReport(report: Report): Boolean {
        // Verifica que el usuario esté autenticado.
        val currentUser = auth.currentUser ?: return false

        // Obtén la fecha actual del servidor.
        val today: LocalDate = getServerDateUseCase()
        val zoneId = ZoneId.systemDefault()
        val startOfDay = today.atStartOfDay(zoneId).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant()

        try {
            // Consulta los reportes del usuario en el día actual.
            val querySnapshot = reportsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereGreaterThanOrEqualTo("createdAt", com.google.firebase.Timestamp(java.util.Date.from(startOfDay)))
                .whereLessThan("createdAt", com.google.firebase.Timestamp(java.util.Date.from(endOfDay)))
                .get()
                .await()

            if (querySnapshot.size() >= 20) {
                // Límite diario alcanzado.
                return false
            }

            // Genera una nueva referencia de documento para asignar el reportId de forma atómica.
            val newDocRef = reportsCollection.document()

            // Completa los datos del reporte. Se asume que en el modelo Report el campo 'createdAt' es de tipo Timestamp?.
            val reportWithDefaults = report.copy(
                reportId = newDocRef.id,
                //userId = currentUser.uid,
                //email = currentUser.email ?: "",
                //phone = currentUser.phoneNumber ?: "",
                // Se puede obtener 'sector' de otra fuente (por ejemplo, del usuario) si se desea; aquí se deja vacío.
                //sector = "",
                // Si 'createdAt' es nulo, se asigna la fecha/hora actual.
                createdAt = report.createdAt ?: com.google.firebase.Timestamp.now()
            )

            // Guarda el reporte en Firestore utilizando la referencia generada.
            newDocRef.set(reportWithDefaults).await()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

}
