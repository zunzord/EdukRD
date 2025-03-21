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
import javax.inject.Inject

data class ExamState(
    val examId: String = "",
    val courseId: String = "",
    val passingScore: Int = 0,
    val totalQuestions: Int = 0,
    val questions: List<Map<String, Any>> = emptyList()
)

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
}
