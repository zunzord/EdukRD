package com.edukrd.app.repository

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.database.ServerValue

/**
 * Provee el offset (en milisegundos) entre el reloj del dispositivo y el reloj del servidor Firebase.
 */
interface TimeRepository {
    suspend fun fetchServerOffsetMillis(): Long
}

@Singleton
class TimeRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) : TimeRepository {

    override suspend fun fetchServerOffsetMillis(): Long {
        val ref = firebaseDatabase.reference
        // 1. Genera una referencia temporal
        val pushedRef = ref.push()

        // 2. Escribe el timestamp del servidor y ESPERA a que se complete
        pushedRef.setValue(ServerValue.TIMESTAMP).await()

        // 3. Lee el valor actualizado desde el servidor
        val serverTime = pushedRef.get().await().getValue(Long::class.java) ?: 0L

        // 4. Elimina el nodo temporal (opcional, pero recomendado)
        pushedRef.removeValue().await()

        return serverTime - System.currentTimeMillis()
    }
}
