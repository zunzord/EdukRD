package com.edukrd.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.models.Course
import com.edukrd.app.repository.CoinRepository
import com.edukrd.app.repository.CourseRepository
import com.edukrd.app.repository.ExamRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

import javax.inject.Inject

data class ExamState(
    val examId: String = "",
    val courseId: String = "",
    val passingScore: Int = 0,
    val totalQuestions: Int = 0,
    val questions: List<Map<String, Any>> = emptyList()
)

data class UserGoalsState(
    val dailyTarget: Int = 1,
    val dailyCurrent: Int = 0,

    val weeklyTarget: Int = 1,
    val weeklyCurrent: Int = 0,

    val monthlyTarget: Int = 1,
    val monthlyCurrent: Int = 0,

    /**
     * Porcentaje global de progreso, de 0.0 a más de 100.0
     * si supera las metas. (Ej: 120% si excede bastante).
     */
    val globalProgress: Float = 0f
)

data class DailyTarget(val target: Int, val streakCount: Int)

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    private val courseRepository: CourseRepository,
    private val coinRepository: CoinRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _examState = MutableStateFlow<ExamState?>(null)
    val examState: StateFlow<ExamState?> = _examState

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _submitResult = MutableStateFlow<Pair<Boolean, String>?>(null)
    val submitResult: StateFlow<Pair<Boolean, String>?> = _submitResult

    private val _dailyTarget = MutableStateFlow(DailyTarget(target = 1, streakCount = 0))
    val dailyTarget: StateFlow<DailyTarget> = _dailyTarget

    private val _userGoalsState = MutableStateFlow(UserGoalsState())
    val userGoalsState: StateFlow<UserGoalsState> = _userGoalsState

    init {
        loadDailyStreakTarget()
    }

    fun loadExamData(courseId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val exam = examRepository.getExamByCourseId(courseId)
                if (exam != null) {
                    val questions = examRepository.getQuestionsForExam(exam.id)
                    _examState.value = ExamState(
                        examId = exam.id,
                        courseId = exam.courseId,
                        passingScore = exam.passingScore,
                        totalQuestions = exam.totalQuestions,
                        questions = questions
                    )
                } else {
                    _error.value = "No se encontró examen para este curso."
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
            _loading.value = false
        }
    }

    fun submitExamResult(courseId: String, score: Int, passed: Boolean) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                _submitResult.value = Pair(false, "Usuario no autenticado")
                _loading.value = false
                return@launch
            }

            val course = courseRepository.getCourseById(courseId)
            val coinsAwarded = if (passed && course != null) {
                coinRepository.awardCoinsForCourse(uid, course)
            } else 0

            val saved = examRepository.submitExamResult(uid, courseId, score, passed)
            if (!saved) {
                _submitResult.value = Pair(false, "Error al enviar el examen.")
            } else if (passed) {
                if (coinsAwarded > 0 && course != null) {
                    coinRepository.updateUserCoins(uid, coinsAwarded)
                    coinRepository.logCoinTransaction(uid, course, coinsAwarded)
                    _submitResult.value = Pair(true, "¡Felicidades! Aprobaste y ganaste $coinsAwarded monedas 🎉")
                } else {
                    _submitResult.value = Pair(true, "¡Felicidades! Aprobaste, pero ya alcanzaste el límite diario de monedas. Podrás seguir ganando mañana.")
                }
            } else {
                _submitResult.value = Pair(false, "No aprobaste. Tu puntuación fue $score%. Inténtalo nuevamente.")
            }

            _loading.value = false
        }
    }

    fun resetSubmitResult() {
        _submitResult.value = null
    }

    fun loadDailyStreakTarget() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            try {
                val dailyCounts = examRepository.getDailyPassedExamCounts(uid)
                if (dailyCounts.isEmpty()) {
                    // No hay registros
                    _dailyTarget.value = DailyTarget(1, 0)
                    return@launch
                }

                // Ordena las fechas de exámenes aprobados
                val sortedDates = dailyCounts.keys.sortedDescending()

                // Promedio global
                val globalAvg = dailyCounts.values.sum().toDouble() / sortedDates.size

                // Promedio semanal
                val last7 = LocalDate.now().minusDays(7)
                val weeklyValues = dailyCounts.filterKeys { it.isAfter(last7) }.values
                val weeklyAvg = if (weeklyValues.isEmpty()) globalAvg else weeklyValues.average()

                // 3) Usa Math en vez de minOf / maxOf
                val targetCalculated = if (weeklyAvg < globalAvg) {
                    Math.max((weeklyAvg * 1.15).toInt(), 1)
                } else {
                    val alt1 = (globalAvg * 1.20).toInt()
                    val alt2 = (weeklyAvg * 1.10).toInt()
                    val best = Math.min(alt1, alt2)
                    Math.max(best, 1)
                }

                // 4) Calcula el streak
                var streak = 0
                sortedDates.forEachIndexed { index, date ->
                    // Rompe si la fecha ya no coincide con "hoy - index"
                    if (date == LocalDate.now().minusDays(index.toLong())) streak++
                    else return@forEachIndexed
                }

                _dailyTarget.value = DailyTarget(targetCalculated, streak)
            } catch (e: Exception) {
                Log.e("ExamViewModel", "Error calculando DailyTarget", e)
            }
        }

    }

    fun loadUserGoals() {
        viewModelScope.launch {
            // Aseguramos que el usuario esté autenticado
            val uid = auth.currentUser?.uid ?: return@launch

            try {
                // 1) Obtenemos todas las fechas de exámenes aprobados
                val allDates = examRepository.getAllPassedExamDates(uid)

                // Si no hay datos, establecemos valores mínimos fijos
                if (allDates.isEmpty()) {
                    _userGoalsState.value = UserGoalsState(
                        dailyTarget = 1,
                        dailyCurrent = 0,
                        weeklyTarget = 7,
                        weeklyCurrent = 0,
                        monthlyTarget = 30,
                        monthlyCurrent = 0,
                        globalProgress = 0f
                    )
                    return@launch
                }

                val today = LocalDate.now()

                // ---------------------------
                // 2) Cálculo de la Meta Diaria
                // Usamos la semana completa anterior.
                val currentWeek = today.get(java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR)
                val lastWeekNumber = if (currentWeek > 1) currentWeek - 1
                else today.minusWeeks(1).get(java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR)
                val lastWeekYear = if (currentWeek > 1) today.year else today.minusWeeks(1).year

                val lastWeekDates = allDates.filter { date ->
                    val week = date.get(java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR)
                    date.year == lastWeekYear && week == lastWeekNumber
                }
                val totalLastWeek = lastWeekDates.size
                // Meta diaria: promedio de la semana anterior, redondeado hacia arriba; mínimo 1.
                val dailyTarget = if (totalLastWeek > 0)
                    kotlin.math.max(1, kotlin.math.ceil(totalLastWeek.toDouble() / 7.0).toInt())
                else 1

                // ---------------------------
                // 3) Cálculo de la Meta Semanal
                // Usamos el mes completo anterior.
                val lastMonthDate = today.minusMonths(1)
                val lastMonth = lastMonthDate.monthValue
                val lastMonthYear = lastMonthDate.year

                val lastMonthDates = allDates.filter { date ->
                    date.year == lastMonthYear && date.monthValue == lastMonth
                }
                val totalLastMonth = lastMonthDates.size
                // Calculamos la cantidad de semanas (distintas) en el mes anterior.
                val weeksInLastMonth = lastMonthDates.map { it.get(java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR) }
                    .toSet().size
                // Si no se encontraron semanas (p.ej., sin datos), usamos 0 para evitar división por cero.
                val computedWeekly = if (weeksInLastMonth > 0)
                    kotlin.math.ceil(totalLastMonth.toDouble() / weeksInLastMonth).toInt() else 0
                // El mínimo para la meta semanal es 7.
                val weeklyTarget = kotlin.math.max(7, computedWeekly)

                // ---------------------------
                // 4) Cálculo de la Meta Mensual
                // Para la meta mensual usamos el total del mes anterior, con un mínimo de 30.
                val monthlyTarget = kotlin.math.max(30, totalLastMonth)

                // ---------------------------
                // 5) Contamos los exámenes aprobados en el periodo actual.
                // Contador diario: cuántos exámenes se aprobaron hoy.
                val currentDaily = allDates.count { it == today }

                // Contador semanal: exámenes aprobados en la semana actual.
                val currentWeekNumber = today.get(java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR)
                val currentWeekly = allDates.filter { date ->
                    date.year == today.year &&
                            date.get(java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR) == currentWeekNumber
                }.size

                // Contador mensual: exámenes aprobados en el mes actual.
                val currentMonth = today.monthValue
                val currentMonthly = allDates.filter { date ->
                    date.year == today.year && date.monthValue == currentMonth
                }.size

                // ---------------------------
                // 6) Cálculo del Progreso Global en porcentaje.
                // Se calcula como el promedio de la fracción alcanzada en cada horizonte.
                val fractionDaily = if (dailyTarget > 0) currentDaily.toFloat() / dailyTarget else 0f
                val fractionWeekly = if (weeklyTarget > 0) currentWeekly.toFloat() / weeklyTarget else 0f
                val fractionMonthly = if (monthlyTarget > 0) currentMonthly.toFloat() / monthlyTarget else 0f

                val globalProgress = ((fractionDaily + fractionWeekly + fractionMonthly) / 3f) * 100f

                // ---------------------------
                // 7) Actualizamos el StateFlow para reactivar la UI.
                _userGoalsState.value = UserGoalsState(
                    dailyTarget = dailyTarget,
                    dailyCurrent = currentDaily,
                    weeklyTarget = weeklyTarget,
                    weeklyCurrent = currentWeekly,
                    monthlyTarget = monthlyTarget,
                    monthlyCurrent = currentMonthly,
                    globalProgress = globalProgress
                )

            } catch (e: Exception) {
                Log.e("ExamViewModel", "Error calculando metas del usuario", e)
                // Aquí podrías emitir un estado de error si lo requieres.
            }
        }
    }


}


