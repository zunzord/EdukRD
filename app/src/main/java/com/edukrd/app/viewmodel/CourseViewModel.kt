    package com.edukrd.app.viewmodel

    import android.util.Log
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.edukrd.app.models.Course
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
        private val auth: FirebaseAuth
    ) : ViewModel() {

        private val _courses = MutableStateFlow<List<Course>>(emptyList())
        val courses: StateFlow<List<Course>> = _courses

        private val _passedCourseIds = MutableStateFlow<Set<String>>(emptySet())
        val passedCourseIds: StateFlow<Set<String>> = _passedCourseIds

        private val _loading = MutableStateFlow(false)
        val loading: StateFlow<Boolean> = _loading

        private val _error = MutableStateFlow<String?>(null)
        val error: StateFlow<String?> = _error

        // Estado para un curso seleccionado
        private val _selectedCourse = MutableStateFlow<Course?>(null)
        val selectedCourse: StateFlow<Course?> = _selectedCourse

        // Contenido del curso
        private val _courseContent = MutableStateFlow<List<Map<String, Any>>>(emptyList())
        val courseContent: StateFlow<List<Map<String, Any>>> = _courseContent

        /**
         * Carga la lista de cursos y la lista de IDs de cursos aprobados.
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
                        val passedIds = courseRepository.getPassedCourseIds(uid)
                        _passedCourseIds.value = passedIds
                    }
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error al cargar cursos o progreso", e)
                    _error.value = e.message
                }
                _loading.value = false
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
                        val contentList = courseRepository.getCourseContent(courseId)
                        _courseContent.value = contentList
                    } else {
                        _error.value = "Curso no encontrado"
                        _selectedCourse.value = null
                    }
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error al obtener el curso", e)
                    _error.value = "Error al cargar el curso: ${e.message}"
                    _selectedCourse.value = null
                }
                _loading.value = false
            }
        }

        /**
         * Inicia el progreso de un curso para el usuario actual,
         * delegando la lógica al repositorio.
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
    }
