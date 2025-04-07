package com.edukrd.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

private const val DATASTORE_NAME = "session_prefs"
private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class SessionPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LOGIN_DATE_KEY = longPreferencesKey("login_date")
        private val SESSION_ID_KEY = stringPreferencesKey("session_id")
    }

    /**
     * Guarda la fecha del login utilizando el epoch day de la fecha.
     */
    suspend fun saveLoginDate(date: LocalDate) {
        context.dataStore.edit { prefs ->
            prefs[LOGIN_DATE_KEY] = date.toEpochDay()
        }
    }

    /**
     * Obtiene la fecha del login guardada, si existe.
     */
    suspend fun getLoginDate(): LocalDate? {
        val epochDay = context.dataStore.data.first()[LOGIN_DATE_KEY]
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }

    /**
     * Guarda el sessionId obtenido al crear una sesión.
     */
    suspend fun saveSessionId(sessionId: String) {
        context.dataStore.edit { prefs ->
            prefs[SESSION_ID_KEY] = sessionId
        }
    }

    /**
     * Recupera el sessionId guardado, si existe.
     */
    suspend fun getSessionId(): String? {
        return context.dataStore.data.first()[SESSION_ID_KEY]
    }

    /**
     * Elimina el sessionId guardado.
     */
    suspend fun clearSessionId() {
        context.dataStore.edit { prefs ->
            prefs.remove(SESSION_ID_KEY)
        }
    }
}
