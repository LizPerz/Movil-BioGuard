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
        val ACCESS_PERMISSIONS = stringPreferencesKey("access_permissions")
        val CAREGIVER_ACCESS_LEVEL = stringPreferencesKey("caregiver_access_level")
        val PLAN_NAME = stringPreferencesKey("plan_name")
        val THEME = stringPreferencesKey("theme")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        
        // Sincronización y Guardián Nocturno
        val IS_SYNC_ENABLED = booleanPreferencesKey("is_sync_enabled")
        val SYNC_INTERVAL_MINUTES = intPreferencesKey("sync_interval_minutes")
        val BATCH_START_HOUR = intPreferencesKey("batch_start_hour")
        val BATCH_END_HOUR = intPreferencesKey("batch_end_hour")
        val IS_BATCH_SYNC_ENABLED = booleanPreferencesKey("is_batch_sync_enabled")
        val IS_NIGHT_GUARDIAN_ENABLED = booleanPreferencesKey("is_night_guardian_enabled")
        val NIGHT_GUARDIAN_START_HOUR = intPreferencesKey("night_guardian_start_hour")
        val NIGHT_GUARDIAN_END_HOUR = intPreferencesKey("night_guardian_end_hour")
        val IS_LOCAL_ALERTS_ENABLED = booleanPreferencesKey("is_local_alerts_enabled")
        val IS_LOCAL_ANALYSIS_ENABLED = booleanPreferencesKey("is_local_analysis_enabled")
        
        // Dispositivo vinculado
        val DEVICE_ID = stringPreferencesKey("device_id")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val DEVICE_NODE_ID = stringPreferencesKey("device_node_id")
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
    val accessPermissions: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACCESS_PERMISSIONS]
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }
    val caregiverAccessLevel: Flow<String?> = context.dataStore.data.map { it[Keys.CAREGIVER_ACCESS_LEVEL] }
    val planName: Flow<String?> = context.dataStore.data.map { it[Keys.PLAN_NAME] }
    val theme: Flow<String?> = context.dataStore.data.map { it[Keys.THEME] }
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_DARK_MODE] ?: true }

    // Getters con valores por defecto
    val isSyncEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_SYNC_ENABLED] ?: true }
    val syncIntervalMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.SYNC_INTERVAL_MINUTES] ?: 15 }
    val batchStartHour: Flow<Int> = context.dataStore.data.map { it[Keys.BATCH_START_HOUR] ?: 2 }
    val batchEndHour: Flow<Int> = context.dataStore.data.map { it[Keys.BATCH_END_HOUR] ?: 6 }
    val isBatchSyncEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_BATCH_SYNC_ENABLED] ?: false }
    val isNightGuardianEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_NIGHT_GUARDIAN_ENABLED] ?: true }
    val nightGuardianStartHour: Flow<Int> = context.dataStore.data.map { it[Keys.NIGHT_GUARDIAN_START_HOUR] ?: 22 }
    val nightGuardianEndHour: Flow<Int> = context.dataStore.data.map { it[Keys.NIGHT_GUARDIAN_END_HOUR] ?: 6 }
    val isLocalAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_LOCAL_ALERTS_ENABLED] ?: true }
    val isLocalAnalysisEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_LOCAL_ANALYSIS_ENABLED] ?: true }

    val deviceId: Flow<String?> = context.dataStore.data.map { it[Keys.DEVICE_ID] }
    val deviceName: Flow<String?> = context.dataStore.data.map { it[Keys.DEVICE_NAME] }
    val deviceNodeId: Flow<String?> = context.dataStore.data.map { it[Keys.DEVICE_NODE_ID] }
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

    suspend fun saveEffectiveAccess(
        patientId: String?,
        caregiverAccessLevel: String?,
        planName: String?,
        permissionCodes: Set<String>
    ) {
        context.dataStore.edit { prefs ->
            if (patientId.isNullOrBlank()) prefs.remove(Keys.PATIENT_ID)
            else prefs[Keys.PATIENT_ID] = patientId
            if (caregiverAccessLevel.isNullOrBlank()) prefs.remove(Keys.CAREGIVER_ACCESS_LEVEL)
            else prefs[Keys.CAREGIVER_ACCESS_LEVEL] = caregiverAccessLevel
            if (planName.isNullOrBlank()) prefs.remove(Keys.PLAN_NAME)
            else prefs[Keys.PLAN_NAME] = planName
            prefs[Keys.ACCESS_PERMISSIONS] = permissionCodes.sorted().joinToString(",")
        }
    }

    suspend fun saveDeviceData(
        deviceId: String,
        deviceName: String,
        isConnected: Boolean = true,
        nodeId: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEVICE_ID] = deviceId
            prefs[Keys.DEVICE_NAME] = deviceName
            prefs[Keys.IS_DEVICE_CONNECTED] = isConnected
            nodeId?.takeIf { it.isNotBlank() }?.let { prefs[Keys.DEVICE_NODE_ID] = it }
        }
    }

    suspend fun clearDeviceData() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.DEVICE_ID)
            prefs.remove(Keys.DEVICE_NAME)
            prefs.remove(Keys.DEVICE_NODE_ID)
            prefs[Keys.IS_DEVICE_CONNECTED] = false
        }
    }

    suspend fun saveSyncSettings(
        isSyncEnabled: Boolean,
        syncIntervalMinutes: Int,
        batchStartHour: Int,
        batchEndHour: Int,
        isBatchSyncEnabled: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_SYNC_ENABLED] = isSyncEnabled
            prefs[Keys.SYNC_INTERVAL_MINUTES] = syncIntervalMinutes
            prefs[Keys.BATCH_START_HOUR] = batchStartHour
            prefs[Keys.BATCH_END_HOUR] = batchEndHour
            prefs[Keys.IS_BATCH_SYNC_ENABLED] = isBatchSyncEnabled
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

    suspend fun saveLocalAnalysisSettings(alertsEnabled: Boolean, analysisEnabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOCAL_ALERTS_ENABLED] = alertsEnabled
            prefs[Keys.IS_LOCAL_ANALYSIS_ENABLED] = analysisEnabled
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_ROLE)
            prefs.remove(Keys.PATIENT_ID)
            prefs.remove(Keys.ACCESS_PERMISSIONS)
            prefs.remove(Keys.CAREGIVER_ACCESS_LEVEL)
            prefs.remove(Keys.PLAN_NAME)
            prefs.remove(Keys.PATIENT_BIRTH_DATE)
            prefs.remove(Keys.PATIENT_SEX)
            prefs.remove(Keys.PATIENT_WEIGHT)
            prefs.remove(Keys.PATIENT_HEIGHT)
            prefs.remove(Keys.PATIENT_IS_DIABETIC)
            prefs.remove(Keys.PATIENT_FAMILY_DIABETES)
            prefs.remove(Keys.PATIENT_ACTIVITY_LEVEL)
        }
    }
}
