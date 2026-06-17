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
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER_JSON = stringPreferencesKey("user_json")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_TOKEN]
    }

    val userJsonFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_JSON]
    }

    /** 同步读取 token（供 OkHttp 拦截器使用） */
    fun getTokenSync(): String? = runBlocking {
        context.dataStore.data.first()[KEY_TOKEN]
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs -> prefs[KEY_TOKEN] = token }
    }

    suspend fun saveUserJson(json: String) {
        context.dataStore.edit { prefs -> prefs[KEY_USER_JSON] = json }
    }

    suspend fun saveAuth(token: String, userJson: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_USER_JSON] = userJson
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
