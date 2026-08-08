package com.bioguard.movil.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurityUtils {

    private const val KEY_ALIAS = "BioGuardMasterKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PREFS_FILE = "bioguard_secure_prefs"
    private const val DB_PASSPHRASE_KEY = "bioguard_db_passphrase"

    fun getMasterKey(context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    fun getEncryptedSharedPreferences(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        getMasterKey(context),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val prefs = getEncryptedSharedPreferences(context)
        var existingPassphraseHex = prefs.getString(DB_PASSPHRASE_KEY, null)

        if (existingPassphraseHex == null) {
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            existingPassphraseHex = randomBytes.toHexString()
            prefs.edit().putString(DB_PASSPHRASE_KEY, existingPassphraseHex).apply()
        }

        return existingPassphraseHex.hexToByteArray()
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }

    private fun String.hexToByteArray(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
