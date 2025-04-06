package com.edukrd.app.usecase

import com.edukrd.app.models.UserGoalsState
import com.edukrd.app.repository.ExamRepository
import com.google.firebase.auth.FirebaseAuth
import com.edukrd.app.usecase.GetServerDateUseCase
import java.time.LocalDate
import java.time.temporal.ChronoField
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.max
import android.util.Log

/**
 * Caso de uso para obtener dinámicamente los datos de objetivos del usuario.
 * Utiliza ExamRepository para recuperar las fechas de exámenes aprobados
 * y calcula los valores: dailyTarget, dailyCurrent, weeklyTarget, weeklyCurrent,
 * monthlyTarget, monthlyCurrent y globalProgress.
 * Se utiliza GetServerDateUseCase para obtener la fecha del servidor y así evitar que
 * el usuario manipule la fecha de la aplicación.
 *
 * Cambios realizados:
 *  - Para el objetivo diario: se multiplica el promedio de la semana anterior por 1.5 y se fuerza un mínimo de 3.
 *  - Para el objetivo semanal: se multiplica el promedio (del mes anterior) por 1.2 y se fuerza un mínimo de 7.
 *  - Para el objetivo mensual: se utiliza el total del mes anterior + 1, con un mínimo de 30.
 */
class GetUserGoalsUseCase @Inject constructor(
    private val examRepository: ExamRepository,
    private val auth: FirebaseAuth,
    private val getServerDateUseCase: GetServerDateUseCase // Inyectamos el use case para obtener la fecha del servidor.
) {
    suspend operator fun invoke(): UserGoalsState {
        // Se obtiene la fecha actual desde el servidor.
        val today: LocalDate = getServerDateUseCase()

        // Obtiene todas las fechas de exámenes aprobados para el usuario.
        val uid = auth.currentUser?.uid ?: return UserGoalsState() // Retorna un estado vacío en caso de error
        val allDates = examRepository.getAllPassedExamDates(uid)

        // Si no hay registros, retorna objetivos mínimos.
        if (allDates.isEmpty()) {
            return UserGoalsState(
                dailyTarget = 3,      // Mínimo forzado para diario
                dailyCurrent = 0,
                weeklyTarget = 7,     // Mínimo forzado para semanal
                weeklyCurrent = 0,
                monthlyTarget = 30,   // Mínimo forzado para mensual
                monthlyCurrent = 0,
                globalProgress = 0f
            )
        }

        // ---------------------------
        // Cálculo de la meta diaria.
        // Se utiliza la semana anterior para obtener un promedio diario.
        val currentWeek = today.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
        val lastWeekNumber = if (currentWeek > 1) currentWeek - 1 else today.minusWeeks(1).get(ChronoField.ALIGNED_WEEK_OF_YEAR)
        val lastWeekYear = if (currentWeek > 1) today.year else today.minusWeeks(1).year
        val lastWeekDates = allDates.filter { date ->
            val week = date.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
            date.year == lastWeekYear && week == lastWeekNumber
        }
        val totalLastWeek = lastWeekDates.size
        // Se calcula el promedio diario de la semana anterior y se multiplica por 1.5
        val dailyAvgLastWeek = totalLastWeek.toDouble() / 7.0
        // Se fuerza un mínimo de 3
        val dailyTarget = max(3, ceil(dailyAvgLastWeek * 1.5).toInt())

        // ---------------------------
        // Cálculo de la meta semanal.
        // Se utiliza el mes anterior para obtener un promedio semanal.
        val lastMonthDate = today.minusMonths(1)
        val lastMonth = lastMonthDate.monthValue
        val lastMonthYear = lastMonthDate.year
        val lastMonthDates = allDates.filter { date ->
            date.year == lastMonthYear && date.monthValue == lastMonth
        }
        val totalLastMonth = lastMonthDates.size
        // Para simplificar, asumimos 4 semanas en el mes.
        val weeklyAvgLastMonth = if (totalLastMonth > 0) totalLastMonth.toDouble() / 4.0 else 0.0
        // Se multiplica el promedio por 1.2 y se fuerza un mínimo de 7.
        val weeklyTarget = max(7, ceil(weeklyAvgLastMonth * 1.2).toInt())

        // ---------------------------
        // Cálculo de la meta mensual.
        // Se utiliza el total de exámenes del mes anterior + 1, con un mínimo de 30.
        val monthlyTarget = max(30, totalLastMonth + 1)

        // ---------------------------
        // Contadores actuales (para el día, la semana y el mes actual).
        val currentDaily = allDates.count { it == today }
        val currentWeekNumber = today.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
        val currentWeekly = allDates.filter { date ->
            date.year == today.year && date.get(ChronoField.ALIGNED_WEEK_OF_YEAR) == currentWeekNumber
        }.size
        val currentMonth = today.monthValue
        val currentMonthly = allDates.count { it.year == today.year && it.monthValue == currentMonth }

        // ---------------------------
        // Cálculo de los porcentajes (progresos).
        val fractionDaily = if (dailyTarget > 0) currentDaily.toFloat() / dailyTarget else 0f
        val fractionWeekly = if (weeklyTarget > 0) currentWeekly.toFloat() / weeklyTarget else 0f
        val fractionMonthly = if (monthlyTarget > 0) currentMonthly.toFloat() / monthlyTarget else 0f
        val globalProgress = ((fractionDaily + fractionWeekly + fractionMonthly) / 3f) * 100f

        // Se imprimen logs para depuración (líneas agregadas)
        Log.d("GetUserGoalsUseCase", "Fechas de exámenes aprobados: $allDates")
        Log.d("GetUserGoalsUseCase", "Objetivo diario -> totalLastWeek: $totalLastWeek, dailyAvg: $dailyAvgLastWeek, dailyTarget: $dailyTarget")
        Log.d("GetUserGoalsUseCase", "Objetivo semanal -> totalLastMonth: $totalLastMonth, weeklyAvg: $weeklyAvgLastMonth, weeklyTarget: $weeklyTarget")
        Log.d("GetUserGoalsUseCase", "Objetivo mensual -> totalLastMonth: $totalLastMonth, monthlyTarget: $monthlyTarget")
        Log.d("GetUserGoalsUseCase", "Contadores -> currentDaily: $currentDaily, currentWeekly: $currentWeekly, currentMonthly: $currentMonthly")
        Log.d("GetUserGoalsUseCase", "Fracciones -> daily: $fractionDaily, weekly: $fractionWeekly, monthly: $fractionMonthly, globalProgress: $globalProgress")

        return UserGoalsState(
            dailyTarget = dailyTarget,
            dailyCurrent = currentDaily,
            weeklyTarget = weeklyTarget,
            weeklyCurrent = currentWeekly,
            monthlyTarget = monthlyTarget,
            monthlyCurrent = currentMonthly,
            globalProgress = globalProgress
        )
    }
}
