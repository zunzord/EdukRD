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

    /**
     * Obtiene la referencia a la subcolección de sesiones del usuario actual.
     */
    private fun getSessionsCollection(uid: String) =
        firestore.collection("users").document(uid).collection("sessions")

    /**
     * Crea o actualiza la sesión para el dispositivo actual.
     * - Si existe una sesión activa para este dispositivo, actualiza 'lastUpdate'.
     * - Si no existe ninguna sesión activa, la crea.
     * - Si existe una sesión activa en otro dispositivo, se lanza una excepción para indicar conflicto.
     *
     * @param deviceId Identificador del dispositivo.
     * @return La sesión creada o actualizada o null en caso de error.
     */
    suspend fun createOrUpdateSession(deviceId: String): Session? {
        val currentUser = auth.currentUser ?: return null
        val uid = currentUser.uid
        val sessionsCollection = getSessionsCollection(uid)

        return try {
            // Consulta todas las sesiones activas para el usuario en su subcolección
            val querySnapshot = sessionsCollection
                .whereEqualTo("active", true)
                .get()
                .await()

            val activeSessions = querySnapshot.documents.mapNotNull { it.toObject(Session::class.java) }
            // Busca si ya existe una sesión activa para este dispositivo
            val deviceSession = activeSessions.firstOrNull { it.deviceId == deviceId }

            return if (deviceSession != null) {
                // Actualiza 'lastUpdate' para refrescar la sesión
                sessionsCollection.document(deviceSession.sessionId)
                    .update("lastUpdate", Timestamp.now())
                    .await()
                deviceSession.copy(lastUpdate = Timestamp.now())
            } else if (activeSessions.isNotEmpty()) {
                // Existe al menos una sesión activa pero no en este dispositivo → conflicto
                throw Exception("Conflict: Existe una sesión activa en otro dispositivo")
            } else {
                // No existe sesión activa; se crea una nueva
                val newDocRef = sessionsCollection.document()
                val newSession = Session(
                    sessionId = newDocRef.id,
                    userId = uid,
                    deviceId = deviceId,
                    active = true,
                    createdAt = Timestamp.now(),
                    lastUpdate = Timestamp.now()
                )
                newDocRef.set(newSession).await()
                newSession
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Forza la creación de una nueva sesión: desactiva todas las sesiones activas del usuario y crea una nueva.
     *
     * @param deviceId Identificador del dispositivo.
     * @return La nueva sesión creada o null en caso de error.
     */
    suspend fun forceCreateNewSession(deviceId: String): Session? {
        val currentUser = auth.currentUser ?: return null
        val uid = currentUser.uid
        val sessionsCollection = getSessionsCollection(uid)

        return try {
            deactivateSessions(uid)
            val newDocRef = sessionsCollection.document()
            val newSession = Session(
                sessionId = newDocRef.id,
                userId = uid,
                deviceId = deviceId,
                active = true,
                createdAt = Timestamp.now(),
                lastUpdate = Timestamp.now()
            )
            newDocRef.set(newSession).await()
            newSession
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Desactiva todas las sesiones activas para el usuario especificado.
     *
     * @param userId El UID del usuario.
     */
    suspend fun deactivateSessions(userId: String) {
        try {
            val sessionsCollection = getSessionsCollection(userId)
            val querySnapshot = sessionsCollection
                .whereEqualTo("active", true)
                .get()
                .await()
            for (doc in querySnapshot.documents) {
                doc.reference.update("active", false, "lastUpdate", Timestamp.now()).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Actualiza el campo 'lastUpdate' de la sesión especificada.
     *
     * @param sessionId El id de la sesión a actualizar.
     */
    suspend fun updateSessionLastUpdate(sessionId: String, userId: String) {
        try {
            val sessionsCollection = getSessionsCollection(userId)
            sessionsCollection.document(sessionId)
                .update("lastUpdate", Timestamp.now()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Agrega un listener a la sesión para notificar cambios en tiempo real.
     *
     * @param sessionId El id de la sesión a observar.
     * @param userId El UID del usuario (para acceder a la subcolección).
     * @param onSessionChanged Callback con la sesión actualizada o null en caso de error.
     * @return La ListenerRegistration que permite cancelar la suscripción.
     */
    fun addSessionListener(userId: String, sessionId: String, onSessionChanged: (Session?) -> Unit): ListenerRegistration {
        val sessionsCollection = getSessionsCollection(userId)
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
     * Retorna un Flow que emite la sesión (o null) cada vez que se actualiza.
     *
     * @param userId El UID del usuario.
     * @param sessionId El id de la sesión a observar.
     */
    fun getSessionFlow(userId: String, sessionId: String) = callbackFlow<Session?> {
        val listenerRegistration = addSessionListener(userId, sessionId) { session ->
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
            val sessionsCollection = getSessionsCollection(userId)
            val querySnapshot = sessionsCollection
                .whereEqualTo("active", true)
                .get()
                .await()
            if (querySnapshot.documents.isNotEmpty()) {
                querySnapshot.documents.first().toObject(Session::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
