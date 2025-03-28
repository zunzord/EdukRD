package com.edukrd.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.repository.CoinRepository
import com.edukrd.app.repository.CourseRepository
import com.edukrd.app.repository.ExamRepository
import com.edukrd.app.usecase.GetServerDateUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoField
import javax.inject.Inject
import com.edukrd.app.models.UserGoalsState



data class ExamState(
    val examId: String = "",
    val courseId: String = "",
    val passingScore: Int = 0,
    val totalQuestions: Int = 0,
    val questions: List<Map<String, Any>> = emptyList()
)



data class DailyTarget(val target: Int, val streakCount: Int)

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    private val courseRepository: CourseRepository,
    private val coinRepository: CoinRepository,
    private val auth: FirebaseAuth,
    private val getServerDateUseCase: GetServerDateUseCase
) : ViewModel() {

    private var currentDate: LocalDate? = null

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

    private val _dailyGraphData = MutableStateFlow<List<Float>>(emptyList())
    val dailyGraphData: StateFlow<List<Float>> = _dailyGraphData

    private val _weeklyGraphData = MutableStateFlow<List<Float>>(emptyList())
    val weeklyGraphData: StateFlow<List<Float>> = _weeklyGraphData

    private val _monthlyGraphData = MutableStateFlow<List<Float>>(emptyList())
    val monthlyGraphData: StateFlow<List<Float>> = _monthlyGraphData

    init {
        viewModelScope.launch {
            currentDate = getServerDateUseCase()
            loadDailyStreakTarget()
            loadUserGoals()
        }
    }

    private fun today(): LocalDate = currentDate ?: LocalDate.now()

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
                val last7 = today().minusDays(7)
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
                    if (date == today().minusDays(index.toLong())) streak++
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

                // Si no hay datos, establecemos valores mínimos fijos y vaciamos los datos gráficos
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
                    _dailyGraphData.value = emptyList()
                    _weeklyGraphData.value = emptyList()
                    _monthlyGraphData.value = emptyList()
                    return@launch
                }

                val today = today()

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
                // Si no se encontraron semanas, evitamos división por cero.
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

                // ---------------------------
                // 8) Calcular datos para los gráficos.
                // Datos diarios: se usa la semana actual. Se genera una lista de 7 elementos (por cada día de la semana, 1 = lunes … 7 = domingo)
                val currentWeekDates = allDates.filter { date ->
                    date.year == today.year &&
                            date.get(java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR) == currentWeek
                }
                val dailyGraph = (1..7).map { dayIndex ->
                    currentWeekDates.count { it.dayOfWeek.value == dayIndex }.toFloat()
                }
                _dailyGraphData.value = dailyGraph

                // Datos semanales: se usa el mes actual. Agrupamos por "semana del mes": ((día - 1) / 7) + 1.
                val currentMonthDates = allDates.filter { date ->
                    date.year == today.year && date.monthValue == currentMonth
                }
                val weeksInCurrentMonth = if (currentMonthDates.isNotEmpty())
                    currentMonthDates.maxOf { ((it.dayOfMonth - 1) / 7) + 1 } else 1
                val weeklyGraph = (1..weeksInCurrentMonth).map { week ->
                    currentMonthDates.count { ((it.dayOfMonth - 1) / 7) + 1 == week }.toFloat()
                }
                _weeklyGraphData.value = weeklyGraph

                // Datos mensuales: se usa el año actual. Se genera una lista de 12 elementos (uno por cada mes)
                val currentYearDates = allDates.filter { it.year == today.year }
                val monthlyGraph = (1..12).map { month ->
                    currentYearDates.count { it.monthValue == month }.toFloat()
                }
                _monthlyGraphData.value = monthlyGraph

            } catch (e: Exception) {
                Log.e("ExamViewModel", "Error calculando metas del usuario", e)
                // Aquí podrías emitir un estado de error si lo requieres.
            }
        }
    }



}


