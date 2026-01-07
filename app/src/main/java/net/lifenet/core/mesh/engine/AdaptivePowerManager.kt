package net.lifenet.core.mesh.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import net.lifenet.core.utils.LifenetLog

/**
 * AdaptivePowerManager: Monitors battery and instructs the mesh to slow down taming on low power.
 */
class AdaptivePowerManager(
    private val context: Context,
    private val onPowerProfileChanged: (DiscoveryProfile) -> Unit
) {

    enum class DiscoveryProfile {
        AGGRESSIVE, // High freq (e.g. searching for SOS)
        NORMAL,     // Balanced
        LOW_POWER   // Slow (Battery below 20%)
    }

    private var currentProfile = DiscoveryProfile.NORMAL

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = level * 100 / scale.toFloat()

            evaluateProfile(batteryPct)
        }
    }

    init {
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        LifenetLog.i("PowerManager", "AdaptivePowerManager initialized")
    }

    private fun evaluateProfile(pct: Float) {
        val newProfile = when {
            pct < 20f -> DiscoveryProfile.LOW_POWER
            pct < 50f -> DiscoveryProfile.NORMAL
            else -> DiscoveryProfile.AGGRESSIVE
        }

        if (newProfile != currentProfile) {
            currentProfile = newProfile
            LifenetLog.i("PowerManager", "Switching to power profile: $newProfile (Battery: $pct%)")
            onPowerProfileChanged(newProfile)
        }
    }

    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignored
        }
    }
}
