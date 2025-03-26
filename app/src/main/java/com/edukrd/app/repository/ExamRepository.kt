package com.edukrd.app.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun getExamByCourseId(courseId: String): Exam? {
        return try {
            val snapshot = firestore.collection("exams")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()
            if (!snapshot.isEmpty) {
                val doc = snapshot.documents[0]
                doc.toObject(Exam::class.java)?.copy(id = doc.id)
            } else null
        } catch (e: Exception) {
            Log.e("ExamRepository", "Error getting exam by courseId", e)
            null
        }
    }

    suspend fun getDailyPassedExamCounts(userId: String): Map<LocalDate, Int> {
        return try {
            val snapshot = firestore.collection("examResults")
                .whereEqualTo("userId", userId)
                .whereEqualTo("passed", true)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { doc ->
                    doc.getTimestamp("date")
                        ?.toDate()
                        ?.toInstant()
                        ?.atZone(ZoneId.systemDefault())
                        ?.toLocalDate()
                }
                .groupingBy { it }
                .eachCount()
        } catch (e: Exception) {
            Log.e("ExamRepository", "Error grouping passed exams by day", e)
            emptyMap()
        }
    }

    suspend fun getQuestionsForExam(examId: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("exams")
                .document(examId)
                .collection("questions")
                .orderBy("order")
                .get()
                .await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e("ExamRepository", "Error getting questions for exam", e)
            emptyList()
        }
    }

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
                .set(examResultData)
                .await()
            true
        } catch (e: Exception) {
            Log.e("ExamRepository", "Error submitting exam result", e)
            false
        }
    }

    suspend fun getAllPassedExamDates(userId: String): List<LocalDate> {
        return try {
            val snapshot = firestore.collection("examResults")
                .whereEqualTo("userId", userId)
                .whereEqualTo("passed", true)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.getTimestamp("date")?.toDate()
                    ?.toInstant()
                    ?.atZone(ZoneId.systemDefault())
                    ?.toLocalDate()
            }
        } catch (e: Exception) {
            Log.e("ExamRepository", "Error retrieving exam dates", e)
            emptyList()
        }
    }


}




