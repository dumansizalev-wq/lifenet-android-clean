package net.lifenet.core.lifecycle

import android.util.Log

/**
 * FailureThreshold: Policy engine for node death and survival.
 */
object FailureThreshold {

    const val MIN_BATTERY_FOR_MESH = 3
    const val CRITICAL_MEMORY_THRESHOLD = 0.95f
    const val MAX_CONSECUTIVE_CRASHES = 3

    sealed class FailureType {
        object CRITICAL_BATTERY : FailureType()
        object MEMORY_EXHAUSTION : FailureType()
        object INTEGRITY_BREACH : FailureType()
        object REBOOT_SIGNAL : FailureType()
    }

    fun shouldTriggerDeath(vitals: NodeVitalMonitor.Vitals): FailureType? {
        return when {
            vitals.batteryPct < MIN_BATTERY_FOR_MESH -> FailureType.CRITICAL_BATTERY
            vitals.memoryStress > CRITICAL_MEMORY_THRESHOLD -> FailureType.MEMORY_EXHAUSTION
            else -> null
        }
    }
}
