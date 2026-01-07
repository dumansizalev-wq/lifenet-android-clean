package net.lifenet.core.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * PersistenceManager: Handles saving and restoring protocol states and user preferences.
 */
class PersistenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lifenet_v1_prefs", Context.MODE_PRIVATE)

    fun saveRadioState(radio: String, enabled: Boolean) {
        prefs.edit().putBoolean("radio_$radio", enabled).apply()
    }

    fun getRadioState(radio: String, default: Boolean = false): Boolean {
        return prefs.getBoolean("radio_$radio", default)
    }

    fun saveMode(isDaily: Boolean) {
        prefs.edit().putBoolean("is_daily_mode", isDaily).apply()
    }

    fun isDailyMode(default: Boolean = true): Boolean {
        return prefs.getBoolean("is_daily_mode", default)
    }

    fun savePortalState(enabled: Boolean) {
        prefs.edit().putBoolean("portal_enabled", enabled).apply()
    }

    fun isPortalEnabled(): Boolean {
        return prefs.getBoolean("portal_enabled", false)
    }

    fun saveVsieConsent(granted: Boolean) {
        prefs.edit().putBoolean("vsie_consent_v1", granted).apply()
    }

    fun isVsieConsentGranted(): Boolean {
        return prefs.getBoolean("vsie_consent_v1", false)
    }
}
