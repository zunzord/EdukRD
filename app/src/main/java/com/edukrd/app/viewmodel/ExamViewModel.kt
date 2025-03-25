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
}
