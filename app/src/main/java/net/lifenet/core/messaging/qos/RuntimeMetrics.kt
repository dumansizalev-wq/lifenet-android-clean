package net.lifenet.core.messaging.qos

import net.lifenet.core.mode.LifenetMode

/**
 * Runtime Metrics: QoS hesaplaması için gerçek zamanlı sistem metrikleri
 * 
 * @param currentMode Aktif sistem modu (ASTRA/DISASTER)
 * @param peerCount Aktif bağlı cihaz sayısı
 * @param queueDepth Bekleyen mesaj kuyruğu uzunluğu
 * @param batteryLevel Batarya seviyesi (0-100)
 * @param networkStabilityScore Ağ kararlılık skoru (0.0-1.0)
 */
data class RuntimeMetrics(
    val currentMode: LifenetMode,
    val peerCount: Int,
    val queueDepth: Int,
    val batteryLevel: Int,
    val networkStabilityScore: Float
) {
    init {
        require(batteryLevel in 0..100) { "Battery level must be 0-100" }
        require(networkStabilityScore in 0.0f..1.0f) { "Network stability must be 0.0-1.0" }
        require(peerCount >= 0) { "Peer count cannot be negative" }
        require(queueDepth >= 0) { "Queue depth cannot be negative" }
    }
    
    /**
     * Sistem yüksek yük altında mı?
     */
    fun isHighLoad(): Boolean {
        return peerCount > 20 || queueDepth > 100
    }
    
    /**
     * Batarya kritik seviyede mi?
     */
    fun isBatteryCritical(): Boolean {
        return batteryLevel < 20
    }
    
    /**
     * Ağ kararsız mı?
     */
    fun isNetworkUnstable(): Boolean {
        return networkStabilityScore < 0.5f
    }
}
