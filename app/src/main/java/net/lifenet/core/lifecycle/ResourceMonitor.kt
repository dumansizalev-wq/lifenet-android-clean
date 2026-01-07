package net.lifenet.core.lifecycle

import android.content.Context
import android.os.BatteryManager
import android.util.Log

/**
 * ResourceMonitor: Sistem kaynaklarını (Batarya/Bellek) izler ve kısıtlama tetiklerini yönetir.
 */
class ResourceMonitor(private val context: Context) {

    /**
     * Batarya yüzdesini döner.
     */
    fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    /**
     * Batarya düşük mü kontrol eder (<15%).
     */
    fun isLowPowerMode(): Boolean {
        return getBatteryLevel() < 15
    }

    /**
     * Sistem yüküne göre kısıtlama kararı verir.
     */
    fun shouldThrottle(): Boolean {
        val isLowBatt = isLowPowerMode()
        if (isLowBatt) {
            Log.w("LIFENET", "ResourceMonitor: Low Battery Alert (${getBatteryLevel()}%). Throttling enabled.")
        }
        return isLowBatt
    }
}
