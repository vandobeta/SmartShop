package com.smartshop.sovereign.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smartshop_settings")

/**
 * DataStore for persistent settings (first-launch, shop info, etc.)
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private object PreferencesKeys {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val ADMIN_PASSCODE = stringPreferencesKey("admin_passcode")
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val SHOP_TEL = stringPreferencesKey("shop_tel")
        val SHOP_NICHE = stringPreferencesKey("shop_niche")
        val CASHIER_NAME = stringPreferencesKey("cashier_name")
        val TAX_RATE = longPreferencesKey("tax_rate")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] ?: true
        }

    val shopName: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.SHOP_NAME] ?: "" }

    val shopTel: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.SHOP_TEL] ?: "" }

    val shopNiche: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.SHOP_NICHE] ?: "" }

    val taxRate: Flow<Long> = context.dataStore.data
        .map { it[PreferencesKeys.TAX_RATE] ?: 1800L } // Default 18%

    val animationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.ANIMATIONS_ENABLED] ?: true }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] = false
        }
    }

    suspend fun setAdminPasscode(passcode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ADMIN_PASSCODE] = passcode
        }
    }

    suspend fun setShopInfo(name: String, tel: String, niche: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOP_NAME] = name
            preferences[PreferencesKeys.SHOP_TEL] = tel
            preferences[PreferencesKeys.SHOP_NICHE] = niche
        }
    }

    suspend fun setTaxRate(rate: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TAX_RATE] = rate
        }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANIMATIONS_ENABLED] = enabled
        }
    }

    suspend fun verifyPasscode(passcode: String): Boolean {
        var stored = ""
        context.dataStore.data.collect { prefs ->
            stored = prefs[PreferencesKeys.ADMIN_PASSCODE] ?: ""
        }
        return stored == passcode
    }
}