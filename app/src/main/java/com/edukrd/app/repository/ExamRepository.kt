package com.edukrd.app.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Data class para representar un examen
data class Exam(
    val courseId: String = "",
    val passingScore: Int = 0,
    val totalQuestions: Int = 0,
    val id: String = ""
)

@Singleton
class ExamRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Obtiene el examen asociado a un curso.
     * Se asume que en Firestore existe una colección "exams" con un campo "courseId".
     */
    suspend fun getExamByCourseId(courseId: String): Exam? {
        return try {
            val snapshot = firestore.collection("exams")
                .whereEqualTo("courseId", courseId)
                .get().await()
            if (!snapshot.isEmpty) {
                val doc = snapshot.documents[0]
                doc.toObject(Exam::class.java)?.copy(id = doc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            // Aquí se podría registrar el error
            null
        }
    }

    /**
     * Obtiene la lista de preguntas de un examen.
     * Se asume que las preguntas están en una subcolección "questions" dentro del documento del examen,
     * y que se pueden ordenar por un campo "order".
     */
    suspend fun getQuestionsForExam(examId: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("exams")
                .document(examId)
                .collection("questions")
                .orderBy("order")
                .get().await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            // Aquí se podría registrar el error
            emptyList()
        }
    }

    /**
     * Envía el resultado del examen.
     *
     * @param userId ID del usuario que realizó el examen.
     * @param courseId ID del curso relacionado con el examen.
     * @param score Puntuación obtenida (en porcentaje, por ejemplo).
     * @param passed Indica si el usuario aprobó el examen.
     * @return true si se envió correctamente, false en caso contrario.
     *
     * Nota: La actualización de monedas se puede manejar desde otro repositorio o en un método adicional.
     */
    suspend fun submitExamResult(
        userId: String,
        courseId: String,
        score: Int,
        passed: Boolean
    ): Boolean {
        return try {
            val examResultData = mapOf(
                "userId" to userId,
                "courseId" to courseId,
                "score" to score,
                "passed" to passed,
                "date" to Timestamp.now()
            )
            firestore.collection("examResults")
                .document()
                .set(examResultData).await()
            true
        } catch (e: Exception) {
            // Aquí se podría registrar el error
            false
        }
    }
}
