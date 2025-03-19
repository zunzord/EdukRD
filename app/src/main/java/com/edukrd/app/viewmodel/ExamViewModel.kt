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

// Estado que encapsula los datos del examen y las preguntas asociadas.
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

    // Estado para el resultado del envío del examen (éxito/fallo y mensaje)
    private val _submitResult = MutableStateFlow<Pair<Boolean, String>?>(null)
    val submitResult: StateFlow<Pair<Boolean, String>?> = _submitResult

    /**
     * Carga el examen y las preguntas asociadas a un curso.
     *
     * @param courseId ID del curso para el cual se busca el examen.
     */
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

    /**
     * Envía el resultado del examen y, si está aprobado, otorga monedas al usuario.
     *
     * @param courseId ID del curso relacionado.
     * @param score Puntuación obtenida en el examen (porcentaje, por ejemplo).
     * @param passed Indica si el usuario aprobó el examen.
     */
    fun submitExamResult(courseId: String, score: Int, passed: Boolean) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val uid = auth.currentUser?.uid
            if (uid == null) {
                _error.value = "Usuario no autenticado"
                _loading.value = false
                return@launch
            }

            try {
                // 1. Guardar el resultado del examen.
                val success = examRepository.submitExamResult(uid, courseId, score, passed)
                if (success) {
                    // 2. Si el examen está aprobado, proceder a otorgar monedas.
                    if (passed) {
                        try {
                            // Obtener el curso para conocer los valores de recompensa.
                            val course: Course? = courseRepository.getCourseById(courseId)
                            if (course != null) {
                                // Determinar la cantidad de monedas a otorgar según la lógica definida.
                                val coinsAwarded = coinRepository.awardCoinsForCourse(uid, course)
                                if (coinsAwarded > 0) {
                                    // Actualizar el saldo de monedas del usuario.
                                    val updateOk = coinRepository.updateUserCoins(uid, coinsAwarded)
                                    if (!updateOk) {
                                        Log.e("ExamViewModel", "Error al actualizar monedas del usuario.")
                                    }
                                    // Registrar la transacción de monedas para auditoría.
                                    val logOk = coinRepository.logCoinTransaction(uid, course, coinsAwarded)
                                    if (!logOk) {
                                        Log.e("ExamViewModel", "Error al registrar la transacción de monedas.")
                                    }
                                }
                            } else {
                                Log.e("ExamViewModel", "No se encontró el curso con ID $courseId.")
                            }
                        } catch (e: Exception) {
                            Log.e("ExamViewModel", "Error al otorgar monedas: ${e.message}", e)
                        }
                    }
                    _submitResult.value = Pair(true, "Examen enviado correctamente.")
                } else {
                    _submitResult.value = Pair(false, "Error al enviar el examen.")
                }
            } catch (e: Exception) {
                _error.value = e.message
                _submitResult.value = Pair(false, e.message ?: "Error desconocido.")
            }
            _loading.value = false
        }
    }
}
