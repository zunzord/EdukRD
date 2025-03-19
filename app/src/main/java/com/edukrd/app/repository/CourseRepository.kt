package com.edukrd.app.repository

import android.util.Log
import com.edukrd.app.models.Course
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Función para sanear URLs de Google Drive
    private fun sanitizeDriveUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null

        val drivePatterns = listOf(
            Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)/.*"""),
            Regex("""drive\.google\.com/uc\?id=([a-zA-Z0-9_-]+).*"""),
            Regex("""drive\.usercontent\.google\.com/download\?id=([a-zA-Z0-9_-]+).*""")
        )

        for (pattern in drivePatterns) {
            val matchResult = pattern.find(url)
            if (matchResult != null) {
                val fileId = matchResult.groupValues[1]
                return "https://drive.google.com/uc?export=view&id=$fileId"
            }
        }

        return url
    }

    suspend fun getPassedCourseIds(userId: String): Set<String> {
        return try {
            val snapshot = firestore.collection("examResults")
                .whereEqualTo("userId", userId)
                .whereEqualTo("passed", true)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.getString("courseId") }.toSet()
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error al obtener passedCourseIds", e)
            emptySet()
        }
    }

    suspend fun getCourseContent(courseId: String): List<Map<String, Any>> {
        return try {
            firestore.collection("courses")
                .document(courseId)
                .collection("content")
                .orderBy("order")
                .get()
                .await()
                .documents.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply {
                        this["imageUrl"] = this["imageUrl"]?.let { sanitizeDriveUrl(it.toString()) } ?: ""
                    }
                }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error al obtener contenido del curso $courseId", e)
            emptyList()
        }
    }

    suspend fun getCourses(): List<Course> {
        return try {
            val snapshot = firestore.collection("courses").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Course::class.java)?.copy(
                    id = doc.id,
                    imageUrl = sanitizeDriveUrl(doc.getString("imageUrl")) ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error al obtener cursos", e)
            emptyList()
        }
    }

    suspend fun getCourseById(courseId: String): Course? {
        return try {
            val document = firestore.collection("courses").document(courseId).get().await()
            if (document.exists()) {
                document.toObject(Course::class.java)?.copy(
                    id = document.id,
                    imageUrl = sanitizeDriveUrl(document.getString("imageUrl")) ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error al obtener curso con ID $courseId", e)
            null
        }
    }

    suspend fun startCourse(userId: String, courseId: String) {
        val progressRef = firestore
            .collection("progress")
            .document(userId)
            .collection("courses")
            .document(courseId)

        val snapshot = progressRef.get().await()
        if (!snapshot.exists()) {
            val progressData = mapOf(
                "courseId" to courseId,
                "progressPercentage" to 0,
                "completed" to false,
                "lastAccessed" to System.currentTimeMillis()
            )
            progressRef.set(progressData).await()
        }
    }
}