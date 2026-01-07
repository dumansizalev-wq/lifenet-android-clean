package net.lifenet.core.lifecycle

import android.content.Context
import android.os.BatteryManager
import android.util.Log

/**
 * NodeVitalMonitor: Continuous hardware and resource health tracking.
 */
class NodeVitalMonitor(private val context: Context) {

    data class Vitals(
        val batteryPct: Int,
        val isCharging: Boolean,
        val cpuTemp: Float, // Simplified simulation
        val memoryStress: Float
    )

    fun checkVitals(): Vitals {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
        
        // Memory analysis using ActivityManager
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val memoryStress = 1.0f - (memInfo.availMem.toFloat() / memInfo.totalMem.toFloat())

        return Vitals(level, isCharging, 35.0f, memoryStress)
    }

    fun isCritical(vitals: Vitals): Boolean {
        return vitals.batteryPct < 5 && !vitals.isCharging
    }
}
