package com.edukrd.data.model

import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp

data class Rating(
    val userId: String = "",
    val courseId: String = "",
    val value: Int = 0,
    val feedback: String = "",
    val timestamp: Timestamp? = null
) {
    companion object {
        /**
         * Construye el mapa que se guardará en Firestore usando serverTimestamp()
         * y agregando feedback y serverDate.
         */
        fun toMap(userId: String, courseId: String, value: Int, feedback: String, serverDate: String): Map<String, Any> {
            return mapOf(
                "userId"     to userId,
                "courseId"   to courseId,
                "value"      to value,
                "feedback"   to feedback,
                "serverDate" to serverDate,
                "timestamp"  to FieldValue.serverTimestamp()
            )
        }
    }
}
