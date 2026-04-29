package com.smartshop.app.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AuthManager(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun isPinSet(): Boolean = sharedPreferences.getBoolean(KEY_PIN_SET, false)
    
    fun setPin(pin: String) {
        sharedPreferences.edit()
            .putString(KEY_PIN_HASH, hashPin(pin))
            .putBoolean(KEY_PIN_SET, true)
            .apply()
    }
    
    fun verifyPin(pin: String): Boolean {
        val storedHash = sharedPreferences.getString(KEY_PIN_HASH, null) ?: return false
        return hashPin(pin) == storedHash
    }
    
    fun clearPin() {
        sharedPreferences.edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_PIN_SET, false)
            .apply()
    }
    
    private fun hashPin(pin: String): String {
        // Simple hash for demo - in production use proper cryptographic hashing
        return pin.hashCode().toString()
    }
    
    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SET = "pin_set"
    }
}