package com.example.bioguard_movil.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
        val PATIENT_ID = stringPreferencesKey("patient_id")
        val THEME = stringPreferencesKey("theme")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    val userId: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val userName: Flow<String?> = context.dataStore.data.map { it[Keys.USER_NAME] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ROLE] }
    val patientId: Flow<String?> = context.dataStore.data.map { it[Keys.PATIENT_ID] }
    val theme: Flow<String?> = context.dataStore.data.map { it[Keys.THEME] }
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_DARK_MODE] ?: true }

    suspend fun saveUserData(userId: String, userName: String, userRole: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USER_NAME] = userName
            prefs[Keys.USER_ROLE] = userRole
        }
    }

    suspend fun saveTheme(theme: String, isDarkMode: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME] = theme
            prefs[Keys.IS_DARK_MODE] = isDarkMode
        }
    }

    suspend fun savePatientId(patientId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PATIENT_ID] = patientId
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
