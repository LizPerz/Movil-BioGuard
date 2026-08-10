package com.bioguard.movil.util

import android.annotation.SuppressLint
import android.content.Context
import java.util.UUID

object InstallationIdentity {
    private const val PREFERENCES = "bioguard_installation"
    private const val INSTALL_ID = "install_id"

    @SuppressLint("ApplySharedPref")
    fun getOrCreate(context: Context): String {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        preferences.getString(INSTALL_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }
        val generated = UUID.randomUUID().toString()
        check(preferences.edit().putString(INSTALL_ID, generated).commit()) {
            "Could not persist installation identity"
        }
        return generated
    }
}
