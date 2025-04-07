package com.edukrd.app.repository

import com.edukrd.app.models.Session
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // Referencia a la colección "sessions" en la raíz de Firestore.
    private val sessionsCollection = firestore.collection("sessions")

    /**
     * Crea una nueva sesión para el usuario actual.
     * Se genera una nueva referencia de documento para asignar de forma atómica el sessionId.
     *
     * @param deviceId Identificador del dispositivo (por ejemplo, obtenido de alguna API de sistema)
     * @return La sesión creada o null en caso de error.
     */
    suspend fun createSession(deviceId: String): Session? {
        val currentUser = auth.currentUser ?: return null
        // Genera una nueva referencia para la sesión.
        val newDocRef = sessionsCollection.document()
        // Crea la sesión usando el ID generado por Firestore.
        val session = Session(
            sessionId = newDocRef.id,
            userId = currentUser.uid,
            deviceId = deviceId,
            active = true,
            createdAt = Timestamp.now(),
            lastUpdate = Timestamp.now()
        )
        return try {
            newDocRef.set(session).await()
            session
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Desactiva todas las sesiones activas del usuario especificado.
     *
     * @param userId El UID del usuario.
     */
    suspend fun deactivateSessions(userId: String) {
        try {
            val querySnapshot = sessionsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("active", true)
                .get()
                .await()
            for (doc in querySnapshot.documents) {
                // Actualiza el campo "active" a false y registra el último cambio.
                doc.reference.update("active", false, "lastUpdate", Timestamp.now()).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Agrega un listener a la sesión con el sessionId indicado.
     * El callback onSessionChanged se llamará cada vez que se actualice el documento.
     *
     * @param sessionId El id de la sesión que se desea observar.
     * @param onSessionChanged Callback que recibe la sesión actualizada o null en caso de error.
     * @return La ListenerRegistration que permite cancelar la suscripción.
     */
    fun addSessionListener(sessionId: String, onSessionChanged: (Session?) -> Unit): ListenerRegistration {
        val docRef = sessionsCollection.document(sessionId)
        return docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onSessionChanged(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val session = snapshot.toObject(Session::class.java)
                onSessionChanged(session)
            } else {
                onSessionChanged(null)
            }
        }
    }

    /**
     * Actualiza el campo 'lastUpdate' de la sesión especificada.
     *
     * @param sessionId El id de la sesión a actualizar.
     */
    suspend fun updateSessionLastUpdate(sessionId: String) {
        try {
            sessionsCollection.document(sessionId)
                .update("lastUpdate", Timestamp.now()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retorna un Flow que emite la sesión (o null) cada vez que ésta se actualiza.
     * Permite que el ViewModel observe cambios en tiempo real.
     *
     * @param sessionId El id de la sesión a observar.
     */
    fun getSessionFlow(sessionId: String) = callbackFlow<Session?> {
        val listenerRegistration = addSessionListener(sessionId) { session ->
            trySend(session)
        }
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Obtiene la sesión activa del usuario, si existe.
     *
     * @param userId El UID del usuario.
     * @return La sesión activa o null si no existe.
     */
    suspend fun getActiveSession(userId: String): Session? {
        return try {
            val querySnapshot = sessionsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("active", true)
                .get()
                .await()
            if (querySnapshot.documents.isNotEmpty()) {
                querySnapshot.documents[0].toObject(Session::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
