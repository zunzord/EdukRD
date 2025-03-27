package com.edukrd.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import java.time.LocalDate

private const val DATASTORE_NAME = "session_prefs"
private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class SessionPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LOGIN_DATE_KEY = longPreferencesKey("login_date")
    }

    suspend fun saveLoginDate(date: LocalDate) {
        context.dataStore.edit { prefs ->
            prefs[LOGIN_DATE_KEY] = date.toEpochDay()
        }
    }

    suspend fun getLoginDate(): LocalDate? {
        val epochDay = context.dataStore.data.first()[LOGIN_DATE_KEY]
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }
}
