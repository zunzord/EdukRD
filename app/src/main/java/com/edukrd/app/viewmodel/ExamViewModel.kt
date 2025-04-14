package com.edukrd.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.models.UserGoalsState
import com.edukrd.app.repository.CoinRepository
import com.edukrd.app.repository.CourseRepository
import com.edukrd.app.repository.ExamRepository
import com.edukrd.app.usecase.GetServerDateUseCase
import com.edukrd.app.usecase.GetUserGoalsUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoField
import javax.inject.Inject

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
    private val getServerDateUseCase: GetServerDateUseCase,
    private val getUserGoalsUseCase: GetUserGoalsUseCase
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
            // Se obtiene la fecha del servidor; si falla, se usará la local.
            currentDate = getServerDateUseCase() ?: LocalDate.now()
            loadDailyStreakTarget()
            loadUserGoals()
            _userGoalsState.value = getUserGoalsUseCase()
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
                    // Obtiene las preguntas originales
                    val originalQuestions = examRepository.getQuestionsForExam(exam.id)
                    // Randomiza el orden de las preguntas
                    val randomizedQuestions = originalQuestions.shuffled().map { question ->
                        val originalOptions = question["answers"] as? List<String> ?: emptyList()
                        val randomizedOptions = originalOptions.shuffled()
                        val originalCorrectIndex = (question["correctOption"] as? Long)?.toInt() ?: -1
                        val correctAnswer = if (originalCorrectIndex in originalOptions.indices) {
                            originalOptions[originalCorrectIndex]
                        } else {
                            ""
                        }
                        val newCorrectIndex = randomizedOptions.indexOf(correctAnswer)
                        question + mapOf(
                            "randomizedOptions" to randomizedOptions,
                            "newCorrectOption" to newCorrectIndex
                        )
                    }
                    _examState.value = ExamState(
                        examId = exam.id,
                        courseId = exam.courseId,
                        passingScore = exam.passingScore,
                        totalQuestions = exam.totalQuestions,
                        questions = randomizedQuestions
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
                    _submitResult.value = Pair(true, "¡Felicidades! Aprobaste y ganaste $coinsAwarded monedas 💰🎉. " +
                            "Visita la Tienda para canjear tus monedas y verificar tu saldo, " +
                            "o consulta la sección de Ranking para conocer tu posición 🥇.")
                } else {
                    _submitResult.value = Pair(true, "¡Felicidades! Aprobaste, pero ya alcanzaste el límite diario de monedas. " +
                            "Podrás seguir ganando mañana. Recuerda que puedes visitar la Tienda para consultar tu saldo " +
                            "y la sección de Ranking para ver tu posición.")
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
                    _dailyTarget.value = DailyTarget(1, 0)
                    return@launch
                }

                val sortedDates = dailyCounts.keys.sortedDescending()
                val globalAvg = dailyCounts.values.sum().toDouble() / sortedDates.size
                val last7 = today().minusDays(7)
                val weeklyValues = dailyCounts.filterKeys { it.isAfter(last7) }.values
                val weeklyAvg = if (weeklyValues.isEmpty()) globalAvg else weeklyValues.average()

                val targetCalculated = if (weeklyAvg < globalAvg) {
                    Math.max((weeklyAvg * 1.15).toInt(), 1)
                } else {
                    val alt1 = (globalAvg * 1.20).toInt()
                    val alt2 = (weeklyAvg * 1.10).toInt()
                    val best = Math.min(alt1, alt2)
                    Math.max(best, 1)
                }

                var streak = 0
                sortedDates.forEachIndexed { index, date ->
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
            val uid = auth.currentUser?.uid ?: return@launch
            try {
                val allDates = examRepository.getAllPassedExamDates(uid)
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

                // --- Actualización de los datos para los gráficos ---
                // Gráfico diario:
                val currentWeekDates = allDates.filter { date ->
                    date.year == today.year &&
                            date.get(ChronoField.ALIGNED_WEEK_OF_YEAR) == today.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
                }
                val dailyGraphRaw = (1..7).map { dayIndex ->
                    currentWeekDates.count { it.dayOfWeek.value == dayIndex }.toFloat()
                }
                // Aseguramos al menos 2 datos para que el gráfico se dibuje correctamente
                _dailyGraphData.value = if (dailyGraphRaw.size < 2) dailyGraphRaw + listOf(0f) else dailyGraphRaw

                // Gráfico semanal:
                val currentMonthDates = allDates.filter { date ->
                    date.year == today.year && date.monthValue == today.monthValue
                }
                val weeksInCurrentMonth = if (currentMonthDates.isNotEmpty())
                    currentMonthDates.maxOf { ((it.dayOfMonth - 1) / 7) + 1 } else 1
                val weeklyGraphRaw = (1..weeksInCurrentMonth).map { week ->
                    currentMonthDates.count { ((it.dayOfMonth - 1) / 7) + 1 == week }.toFloat()
                }
                _weeklyGraphData.value = if (weeklyGraphRaw.size < 2) weeklyGraphRaw + listOf(0f) else weeklyGraphRaw

                // Gráfico mensual:
                val currentYearDates = allDates.filter { it.year == today.year }
                val monthlyGraphRaw = (1..12).map { month ->
                    currentYearDates.count { it.monthValue == month }.toFloat()
                }
                _monthlyGraphData.value = if (monthlyGraphRaw.size < 2) monthlyGraphRaw + listOf(0f) else monthlyGraphRaw

                // --- Actualización de los contadores actuales (sin tocar los targets ya establecidos) ---
                val currentDaily = allDates.count { it == today }
                val currentWeekNumber = today.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
                val currentWeekly = allDates.filter { date ->
                    date.year == today.year && date.get(ChronoField.ALIGNED_WEEK_OF_YEAR) == currentWeekNumber
                }.size
                val currentMonth = today.monthValue
                val currentMonthly = allDates.count { it.year == today.year && it.monthValue == currentMonth }

                // Usamos los targets ya calculados y almacenados en _userGoalsState.value
                val existingGoals = _userGoalsState.value

                val fractionDaily = if (existingGoals.dailyTarget > 0)
                    currentDaily.toFloat() / existingGoals.dailyTarget else 0f
                val fractionWeekly = if (existingGoals.weeklyTarget > 0)
                    currentWeekly.toFloat() / existingGoals.weeklyTarget else 0f
                val fractionMonthly = if (existingGoals.monthlyTarget > 0)
                    currentMonthly.toFloat() / existingGoals.monthlyTarget else 0f
                val globalProgress = ((fractionDaily + fractionWeekly + fractionMonthly) / 3f) * 100f

                // Actualizamos solo los contadores actuales y el progreso global,
                // sin modificar los targets (que provienen del GetUserGoalsUseCase)
                _userGoalsState.value = existingGoals.copy(
                    dailyCurrent = currentDaily,
                    weeklyCurrent = currentWeekly,
                    monthlyCurrent = currentMonthly,
                    globalProgress = globalProgress
                )

            } catch (e: Exception) {
                Log.e("ExamViewModel", "Error calculando metas del usuario", e)
            }
        }
    }
    suspend fun getCoinsEarned(courseId: String): Int {
        val uid = auth.currentUser?.uid ?: return 0
        val course = courseRepository.getCourseById(courseId) ?: return 0
        return coinRepository.awardCoinsForCourse(uid, course)
    }
}
