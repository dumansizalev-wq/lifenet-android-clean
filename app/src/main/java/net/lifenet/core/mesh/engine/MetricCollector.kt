package net.lifenet.core.mesh.engine

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * MetricCollector: Çekirdek motor metriklerini toplar ve UI için hazırlar.
 */
object MetricCollector {
    private val fecRecoveries = AtomicInteger(0)
    private val totalFragments = AtomicInteger(0)
    private val portalMessages = AtomicInteger(0)
    private val qosViolations = AtomicInteger(0)
    private val predictiveReroutes = AtomicInteger(0)
    private val modeSwitchCount = AtomicInteger(0)
    
    // QoS Metrics
    private val qosCriticalSent = AtomicInteger(0)
    private val qosNormalSent = AtomicInteger(0)
    private val qosBulkSent = AtomicInteger(0)
    private val qosBulkDropped = AtomicInteger(0)
    private val qosAverageDelay = AtomicLong(0)
    
    private var ghostWipeAtomicSuccess: Boolean = false
    private var hardwareStatus: String = "IDLE"
    private var currentMode: String = "DAILY"


    private val currentSyncLag = AtomicLong(0)

    fun incrementFecRecovery() = fecRecoveries.incrementAndGet()
    fun incrementTotalFragments() = totalFragments.incrementAndGet()
    fun updateSyncLag(lagMs: Long) = currentSyncLag.set(lagMs)
    fun incrementPortalMessage() = portalMessages.incrementAndGet()
    fun incrementQosViolation() = qosViolations.incrementAndGet()
    fun incrementPredictiveReroute() = predictiveReroutes.incrementAndGet()
    fun incrementModeSwitchCount() = modeSwitchCount.incrementAndGet()
    
    // QoS Metrics
    fun incrementQosCriticalSent() = qosCriticalSent.incrementAndGet()
    fun incrementQosNormalSent() = qosNormalSent.incrementAndGet()
    fun incrementQosBulkSent() = qosBulkSent.incrementAndGet()
    fun incrementQosBulkDropped() = qosBulkDropped.incrementAndGet()
    fun updateQosAverageDelay(delayMs: Long) = qosAverageDelay.set(delayMs)
    
    fun setGhostWipeStatus(success: Boolean) { ghostWipeAtomicSuccess = success }
    fun setHardwareStatus(status: String) { hardwareStatus = status }
    fun setCurrentMode(mode: String) { currentMode = mode }

    /**
     * Tüm verileri bir Harita olarak döner (UI/Bridge için).
     */
    fun getSnapshot(): Map<String, Any> {
        return mapOf(
            "fec_recovery_count" to fecRecoveries.get(),
            "total_fragments" to totalFragments.get(),
            "sync_lag_ms" to currentSyncLag.get(),
            "portal_message_count" to portalMessages.get(),
            "qos_violations" to qosViolations.get(),
            "predictive_reroute_count" to predictiveReroutes.get(),
            "mode_switch_count" to modeSwitchCount.get(),
            "qos_critical_sent" to qosCriticalSent.get(),
            "qos_normal_sent" to qosNormalSent.get(),
            "qos_bulk_sent" to qosBulkSent.get(),
            "qos_bulk_dropped" to qosBulkDropped.get(),
            "qos_average_delay" to qosAverageDelay.get(),
            "ghost_wipe_verified" to ghostWipeAtomicSuccess,
            "current_mode" to currentMode,
            "hardware_status" to hardwareStatus,
            "fec_efficiency" to if (totalFragments.get() > 0) 
                (fecRecoveries.get().toDouble() / totalFragments.get() * 100) else 0.0
        )
    }
}
