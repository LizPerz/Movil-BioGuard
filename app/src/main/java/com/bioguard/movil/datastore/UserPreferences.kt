package com.bioguard.movil.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
        
        // Sincronización y Guardián Nocturno
        val IS_SYNC_ENABLED = booleanPreferencesKey("is_sync_enabled")
        val SYNC_INTERVAL_MINUTES = intPreferencesKey("sync_interval_minutes")
        val BATCH_START_HOUR = intPreferencesKey("batch_start_hour")
        val BATCH_END_HOUR = intPreferencesKey("batch_end_hour")
        val IS_NIGHT_GUARDIAN_ENABLED = booleanPreferencesKey("is_night_guardian_enabled")
        val NIGHT_GUARDIAN_START_HOUR = intPreferencesKey("night_guardian_start_hour")
        val NIGHT_GUARDIAN_END_HOUR = intPreferencesKey("night_guardian_end_hour")
        
        // Dispositivo vinculado
        val DEVICE_ID = stringPreferencesKey("device_id")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val IS_DEVICE_CONNECTED = booleanPreferencesKey("is_device_connected")

        // Perfil Médico / Biometría
        val PATIENT_BIRTH_DATE = stringPreferencesKey("patient_birth_date")
        val PATIENT_SEX = stringPreferencesKey("patient_sex")
        val PATIENT_WEIGHT = stringPreferencesKey("patient_weight")
        val PATIENT_HEIGHT = stringPreferencesKey("patient_height")
        val PATIENT_IS_DIABETIC = booleanPreferencesKey("patient_is_diabetic")
        val PATIENT_FAMILY_DIABETES = booleanPreferencesKey("patient_family_diabetes")
        val PATIENT_ACTIVITY_LEVEL = stringPreferencesKey("patient_activity_level")
    }

    val userId: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val userName: Flow<String?> = context.dataStore.data.map { it[Keys.USER_NAME] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ROLE] }
    val patientId: Flow<String?> = context.dataStore.data.map { it[Keys.PATIENT_ID] }
    val theme: Flow<String?> = context.dataStore.data.map { it[Keys.THEME] }
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_DARK_MODE] ?: true }

    // Getters con valores por defecto
    val isSyncEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_SYNC_ENABLED] ?: true }
    val syncIntervalMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.SYNC_INTERVAL_MINUTES] ?: 15 }
    val batchStartHour: Flow<Int> = context.dataStore.data.map { it[Keys.BATCH_START_HOUR] ?: 2 }
    val batchEndHour: Flow<Int> = context.dataStore.data.map { it[Keys.BATCH_END_HOUR] ?: 6 }
    val isNightGuardianEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_NIGHT_GUARDIAN_ENABLED] ?: true }
    val nightGuardianStartHour: Flow<Int> = context.dataStore.data.map { it[Keys.NIGHT_GUARDIAN_START_HOUR] ?: 22 }
    val nightGuardianEndHour: Flow<Int> = context.dataStore.data.map { it[Keys.NIGHT_GUARDIAN_END_HOUR] ?: 6 }

    val deviceId: Flow<String?> = context.dataStore.data.map { it[Keys.DEVICE_ID] }
    val deviceName: Flow<String?> = context.dataStore.data.map { it[Keys.DEVICE_NAME] }
    val isDeviceConnected: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_DEVICE_CONNECTED] ?: false }

    val patientBirthDate: Flow<String?> = context.dataStore.data.map { it[Keys.PATIENT_BIRTH_DATE] }
    val patientSex: Flow<String?> = context.dataStore.data.map { it[Keys.PATIENT_SEX] }
    val patientWeight: Flow<String?> = context.dataStore.data.map { it[Keys.PATIENT_WEIGHT] }
    val patientHeight: Flow<String?> = context.dataStore.data.map { it[Keys.PATIENT_HEIGHT] }
    val patientIsDiabetic: Flow<Boolean> = context.dataStore.data.map { it[Keys.PATIENT_IS_DIABETIC] ?: false }
    val patientFamilyDiabetes: Flow<Boolean> = context.dataStore.data.map { it[Keys.PATIENT_FAMILY_DIABETES] ?: false }
    val patientActivityLevel: Flow<String?> = context.dataStore.data.map { it[Keys.PATIENT_ACTIVITY_LEVEL] }

    suspend fun savePatientBiometrics(
        birthDate: String,
        sex: String,
        weight: String,
        height: String,
        isDiabetic: Boolean,
        familyDiabetes: Boolean,
        activityLevel: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PATIENT_BIRTH_DATE] = birthDate
            prefs[Keys.PATIENT_SEX] = sex
            prefs[Keys.PATIENT_WEIGHT] = weight
            prefs[Keys.PATIENT_HEIGHT] = height
            prefs[Keys.PATIENT_IS_DIABETIC] = isDiabetic
            prefs[Keys.PATIENT_FAMILY_DIABETES] = familyDiabetes
            prefs[Keys.PATIENT_ACTIVITY_LEVEL] = activityLevel
        }
    }

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

    suspend fun saveDeviceData(deviceId: String, deviceName: String, isConnected: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEVICE_ID] = deviceId
            prefs[Keys.DEVICE_NAME] = deviceName
            prefs[Keys.IS_DEVICE_CONNECTED] = isConnected
        }
    }

    suspend fun clearDeviceData() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.DEVICE_ID)
            prefs.remove(Keys.DEVICE_NAME)
            prefs[Keys.IS_DEVICE_CONNECTED] = false
        }
    }

    suspend fun saveSyncSettings(
        isSyncEnabled: Boolean,
        syncIntervalMinutes: Int,
        batchStartHour: Int,
        batchEndHour: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_SYNC_ENABLED] = isSyncEnabled
            prefs[Keys.SYNC_INTERVAL_MINUTES] = syncIntervalMinutes
            prefs[Keys.BATCH_START_HOUR] = batchStartHour
            prefs[Keys.BATCH_END_HOUR] = batchEndHour
        }
    }

    suspend fun saveNightGuardianSettings(
        isEnabled: Boolean,
        startHour: Int,
        endHour: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_NIGHT_GUARDIAN_ENABLED] = isEnabled
            prefs[Keys.NIGHT_GUARDIAN_START_HOUR] = startHour
            prefs[Keys.NIGHT_GUARDIAN_END_HOUR] = endHour
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

