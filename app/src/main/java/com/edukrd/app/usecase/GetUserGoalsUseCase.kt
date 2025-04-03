package com.edukrd.app.usecase

import com.edukrd.app.models.UserGoalsState
import com.edukrd.app.repository.ExamRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.temporal.ChronoField
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.max

/**
 * Caso de uso para obtener dinámicamente los datos de objetivos del usuario.
 * Utiliza ExamRepository para recuperar las fechas de exámenes aprobados
 * y calcula los valores: dailyTarget, dailyCurrent, weeklyTarget, weeklyCurrent,
 * monthlyTarget, monthlyCurrent y globalProgress.
 *
 * En un escenario real, podrías utilizar GetServerDateUseCase para obtener la fecha del servidor;
 * en este ejemplo se utiliza LocalDate.now().
 */
class GetUserGoalsUseCase @Inject constructor(
    private val examRepository: ExamRepository,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(): UserGoalsState {
        // Obtiene la fecha actual; en producción, podrías usar GetServerDateUseCase().
        val today = LocalDate.now()

        // Obtiene todas las fechas de exámenes aprobados para el usuario.
        val uid = auth.currentUser?.uid ?: return UserGoalsState() // Retorna un estado vacío en caso de error
        val allDates = examRepository.getAllPassedExamDates(uid)

        // Si no hay registros, retorna objetivos mínimos.
        if (allDates.isEmpty()) {
            return UserGoalsState(
                dailyTarget = 1,
                dailyCurrent = 0,
                weeklyTarget = 7,
                weeklyCurrent = 0,
                monthlyTarget = 30,
                monthlyCurrent = 0,
                globalProgress = 0f
            )
        }

        // Cálculo de la meta diaria usando la semana completa anterior.
        val currentWeek = today.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
        val lastWeekNumber = if (currentWeek > 1) currentWeek - 1 else today.minusWeeks(1).get(ChronoField.ALIGNED_WEEK_OF_YEAR)
        val lastWeekYear = if (currentWeek > 1) today.year else today.minusWeeks(1).year
        val lastWeekDates = allDates.filter { date ->
            val week = date.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
            date.year == lastWeekYear && week == lastWeekNumber
        }
        val totalLastWeek = lastWeekDates.size
        val dailyTarget = if (totalLastWeek > 0) max(1, ceil(totalLastWeek.toDouble() / 7.0).toInt()) else 1

        // Cálculo de la meta semanal usando el mes completo anterior.
        val lastMonthDate = today.minusMonths(1)
        val lastMonth = lastMonthDate.monthValue
        val lastMonthYear = lastMonthDate.year
        val lastMonthDates = allDates.filter { date ->
            date.year == lastMonthYear && date.monthValue == lastMonth
        }
        val totalLastMonth = lastMonthDates.size
        val weeksInLastMonth = lastMonthDates.map { it.get(ChronoField.ALIGNED_WEEK_OF_YEAR) }.toSet().size
        val computedWeekly = if (weeksInLastMonth > 0) ceil(totalLastMonth.toDouble() / weeksInLastMonth).toInt() else 0
        val weeklyTarget = max(7, computedWeekly)

        // Cálculo de la meta mensual: total del mes anterior, con un mínimo de 30.
        val monthlyTarget = max(30, totalLastMonth)

        // Contadores actuales.
        val currentDaily = allDates.count { it == today }
        val currentWeekNumber = today.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
        val currentWeekly = allDates.filter { date ->
            date.year == today.year && date.get(ChronoField.ALIGNED_WEEK_OF_YEAR) == currentWeekNumber
        }.size
        val currentMonth = today.monthValue
        val currentMonthly = allDates.filter { date ->
            date.year == today.year && date.monthValue == currentMonth
        }.size

        // Cálculo del progreso global (promedio de la fracción alcanzada en cada horizonte).
        val fractionDaily = if (dailyTarget > 0) currentDaily.toFloat() / dailyTarget else 0f
        val fractionWeekly = if (weeklyTarget > 0) currentWeekly.toFloat() / weeklyTarget else 0f
        val fractionMonthly = if (monthlyTarget > 0) currentMonthly.toFloat() / monthlyTarget else 0f
        val globalProgress = ((fractionDaily + fractionWeekly + fractionMonthly) / 3f) * 100f

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
