package com.edukrd.data.model

import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp

data class Rating(
    val userId: String = "",
    val courseId: String = "",
    val value: Int = 0,
    val timestamp: Timestamp? = null
) {
    companion object {
        /** Construye el mapa que se guardará en Firestore usando serverTimestamp() */
        fun toMap(userId: String, courseId: String, value: Int): Map<String, Any> {
            return mapOf(
                "userId"    to userId,
                "courseId"  to courseId,
                "value"     to value,
                "timestamp" to FieldValue.serverTimestamp()
            )
        }
    }
}
