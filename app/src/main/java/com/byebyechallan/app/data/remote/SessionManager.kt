package com.byebyechallan.app.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "byebyechallan_secure_prefs")

/**
 * Stores the auth token and current user's id on-device so the user doesn't have
 * to log in every time they open the app.
 *
 * Note: DataStore Preferences itself is not encrypted at rest by default. For a
 * production release, encrypt the values before storing them (e.g. with Google
 * Tink) rather than reaching for androidx.security's EncryptedSharedPreferences,
 * which is now deprecated. Flagged in README as a pre-launch task.
 */
class SessionManager(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = longPreferencesKey("user_id")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val userIdFlow: Flow<Long?> = context.dataStore.data.map { it[KEY_USER_ID] }

    suspend fun saveSession(token: String, refreshToken: String?, userId: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            refreshToken?.let { prefs[KEY_REFRESH_TOKEN] = it }
            prefs[KEY_USER_ID] = userId
        }
    }

    suspend fun getToken(): String? = context.dataStore.data.first()[KEY_TOKEN]

    suspend fun getUserId(): Long? = context.dataStore.data.first()[KEY_USER_ID]

    suspend fun isLoggedIn(): Boolean = getToken() != null

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
