package com.vinylstore.app.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vinyl_prefs")

class TokenStorage(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_JSON_KEY = stringPreferencesKey("user_json")
    }

    val tokenFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TOKEN_KEY] ?: ""
    }

    val userJsonFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_JSON_KEY] ?: ""
    }

    fun getTokenSync(): String = runBlocking {
        context.dataStore.data.first()[TOKEN_KEY] ?: ""
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    suspend fun saveUserJson(json: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_JSON_KEY] = json
        }
    }

    suspend fun saveAuth(token: String, userJson: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_JSON_KEY] = userJson
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
