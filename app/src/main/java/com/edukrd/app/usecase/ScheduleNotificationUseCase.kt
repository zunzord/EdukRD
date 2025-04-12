package com.edukrd.app.usecase

import com.edukrd.app.models.NotificationData
import com.edukrd.app.repository.NotificationRepository
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Caso de uso que, basado en la configuración de notificaciones del usuario y los datos de objetivos
 * obtenidos dinámicamente mediante GetUserGoalsUseCase, programa (o cancela) una notificación
 * usando el NotificationRepository.
 *
 * Si las notificaciones están deshabilitadas, se cancela cualquier notificación programada.
 * En caso contrario, se construye un NotificationData con título, mensaje y hora de disparo
 * en función de la frecuencia configurada, y se programa la notificación.
 */
class ScheduleNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val getUserGoalsUseCase: GetUserGoalsUseCase // Caso de uso para obtener los datos dinámicos
) {
    suspend operator fun invoke(
        notificationsEnabled: Boolean,
        frequency: String // "Diaria", "Semanal" o "Mensual"
    ) {
        if (!notificationsEnabled) {
            // Cancela cualquier notificación programada si están deshabilitadas.
            notificationRepository.cancelNotification()
            return
        }

        // Periodo para notificar
        val now = LocalDateTime.now()

        val scheduledTime = when (frequency) {
            "Diaria" -> now.plusDays(1)
            "Semanal" -> now.plusWeeks(1)
            "Mensual" -> now.plusMonths(1)
            else -> now.plusDays(1)
        }

        // fuente de metas
        val goals = getUserGoalsUseCase()

        // Construccion de notificacion
        val (title, message) = when (frequency) {
            "Diaria" -> {
                val percentage = if (goals.dailyTarget > 0) (goals.dailyCurrent * 100 / goals.dailyTarget) else 0
                Pair(
                    "Meta Diaria",
                    "Has completado ${goals.dailyCurrent} de ${goals.dailyTarget} ($percentage%) de tu meta diaria."
                )
            }
            "Semanal" -> {
                val percentage = if (goals.weeklyTarget > 0) (goals.weeklyCurrent * 100 / goals.weeklyTarget) else 0
                Pair(
                    "Meta Semanal",
                    "Has completado ${goals.weeklyCurrent} de ${goals.weeklyTarget} ($percentage%) de tu meta semanal."
                )
            }
            "Mensual" -> {
                val percentage = if (goals.monthlyTarget > 0) (goals.monthlyCurrent * 100 / goals.monthlyTarget) else 0
                Pair(
                    "Meta Mensual",
                    "Has completado ${goals.monthlyCurrent} de ${goals.monthlyTarget} ($percentage%) de tu meta mensual."
                )
            }
            else -> {
                val percentage = if (goals.dailyTarget > 0) (goals.dailyCurrent * 100 / goals.dailyTarget) else 0
                Pair(
                    "Meta Diaria",
                    "Has completado ${goals.dailyCurrent} de ${goals.dailyTarget} ($percentage%) de tu meta diaria."
                )
            }
        }

        // objeto de notificacion
        val notificationData = NotificationData(
            frequency = frequency,
            title = title,
            message = message,
            scheduledTime = scheduledTime
        )

        
        notificationRepository.scheduleNotification(notificationData)
    }
}
