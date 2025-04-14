package com.edukrd.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.models.Course
import com.edukrd.app.repository.CoinRepository
import com.edukrd.app.repository.CourseRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val coinRepository: CoinRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses

    private val _passedCourseIds = MutableStateFlow<Set<String>>(emptySet())
    val passedCourseIds: StateFlow<Set<String>> = _passedCourseIds

    // NUEVO: mapa cursoId → monedas a otorgar hoy
    private val _coinRewards = MutableStateFlow<Map<String, Int>>(emptyMap())
    val coinRewards: StateFlow<Map<String, Int>> = _coinRewards

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse: StateFlow<Course?> = _selectedCourse

    private val _courseContent = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val courseContent: StateFlow<List<Map<String, Any>>> = _courseContent

    private val _courseReferences = MutableStateFlow<List<String>>(emptyList())
    val courseReferences: StateFlow<List<String>> = _courseReferences

    /**
     * Carga la lista de cursos, progreso y calcula las monedas a otorgar por curso hoy.
     */
    fun loadCoursesAndProgress() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val courseList = courseRepository.getCourses()
                _courses.value = courseList

                val uid = auth.currentUser?.uid
                if (uid != null) {
                    _passedCourseIds.value = courseRepository.getPassedCourseIds(uid)

                    // Calcular recompensas para cada curso
                    val rewards = mutableMapOf<String, Int>()
                    courseList.forEach { course ->
                        rewards[course.id] = coinRepository.awardCoinsForCourse(uid, course)
                    }
                    _coinRewards.value = rewards
                }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error al cargar cursos o progreso", e)
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Carga un curso específico por su ID y su contenido.
     */
    fun loadCourseById(courseId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val courseObj = courseRepository.getCourseById(courseId)
                _selectedCourse.value = courseObj

                if (courseObj != null) {
                    _courseContent.value = courseRepository.getCourseContent(courseId)
                } else {
                    _error.value = "Curso no encontrado"
                    _selectedCourse.value = null
                }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error al obtener el curso", e)
                _error.value = "Error al cargar el curso: ${e.message}"
                _selectedCourse.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Inicia el progreso de un curso para el usuario actual.
     */
    fun startCourse(courseId: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            try {
                courseRepository.startCourse(uid, courseId)
                Log.d("CourseViewModel", "Curso iniciado: $courseId")
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error al iniciar curso", e)
            }
        }
    }
    fun loadCoursesByCategory(category: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // Se obtienen todos los cursos
                val courseList = courseRepository.getCourses()
                // Se filtran según la categoría (ignorando mayúsculas/minúsculas)
                val filtered = courseList.filter { it.categoria.equals(category, ignoreCase = true) }
                _courses.value = filtered
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error al cargar cursos por categoría", e)
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
    fun loadReferencesByCourseId(courseId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val courseObj = courseRepository.getCourseById(courseId)
                _courseReferences.value = courseObj?.referencias ?: emptyList()
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error al cargar referencias para curso $courseId", e)
                _error.value = "Error al cargar referencias: ${e.message}"
                _courseReferences.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
