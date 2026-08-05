package com.bioguard.movil.datastore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE_ALIAS = "bioguard_auth_key"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val GCM_TRANSFORMATION = "AES/GCM/NoPadding"
private const val IV_SIZE_BYTES = 12
private const val TAG_LENGTH_BITS = 128

private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_prefs")

class SecureTokenStorage(private val context: Context) {

    private object Keys {
        val AUTH_TOKEN = stringPreferencesKey("enc_auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("enc_refresh_token")
    }

    val authToken: Flow<String?> = context.secureDataStore.data.map { prefs ->
        prefs[Keys.AUTH_TOKEN]?.let { decrypt(it) }
    }

    val refreshToken: Flow<String?> = context.secureDataStore.data.map { prefs ->
        prefs[Keys.REFRESH_TOKEN]?.let { decrypt(it) }
    }

    suspend fun saveAuthToken(token: String) {
        context.secureDataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = encrypt(token)
        }
    }

    suspend fun saveRefreshToken(token: String) {
        context.secureDataStore.edit { prefs ->
            prefs[Keys.REFRESH_TOKEN] = encrypt(token)
        }
    }

    suspend fun clear() {
        context.secureDataStore.edit { it.clear() }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    private fun decrypt(cipherData: String): String? {
        return try {
            val data = Base64.decode(cipherData, Base64.NO_WRAP)
            if (data.size <= IV_SIZE_BYTES) return null
            val iv = data.copyOfRange(0, IV_SIZE_BYTES)
            val cipherText = data.copyOfRange(IV_SIZE_BYTES, data.size)
            val cipher = Cipher.getInstance(GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
