package net.lifenet.core.messaging.qos

import android.util.Log
import net.lifenet.core.mode.LifenetMode
import net.lifenet.core.mode.ModeManager
import net.lifenet.core.lifecycle.ResourceMonitor
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import net.lifenet.core.data.MessageType
import net.lifenet.core.messaging.qos.QoSLevel
import net.lifenet.core.messaging.qos.RuntimeMetrics

/**
 * QoSController: Dinamik QoS yönetimi ve öncelik hesaplama
 * 
 * CONSTITUTIONAL GUARANTEES:
 * - Thread-safe priority calculation
 * - Deterministic behavior (same inputs → same outputs)
 * - Mode-aware (ASTRA vs DISASTER)
 * - No random behavior
 * 
 * ASTRA Mode:
 * - Statik öncelikler
 * - CRITICAL her zaman öncelikli
 * - BULK düşük öncelikli ama durdurulmaz
 * 
 * DISASTER Mode:
 * - Dinamik öncelik ayarlama
 * - Runtime metrics bazlı throttling
 * - Batarya < 20% → BULK stop
 * - Queue depth > 100 → BULK stop
 * - Peer count > 20 → Agresif önceliklendirme
 */
class QoSController(
    private val modeManager: ModeManager,
    private val resourceMonitor: ResourceMonitor
) {
    private val lock = ReentrantReadWriteLock()
    
    // Configurable thresholds
    private val HIGH_PEER_COUNT_THRESHOLD = 20
    private val HIGH_QUEUE_DEPTH_THRESHOLD = 100
    private val CRITICAL_BATTERY_THRESHOLD = 20
    private val UNSTABLE_NETWORK_THRESHOLD = 0.5f
    
    /**
     * Mesaj önceliğini hesaplar.
     * 
     * @param messageType Mesaj tipi
     * @param metrics Runtime metrikleri
     * @return Hesaplanmış öncelik (0-120 arası)
     */
    fun calculateQoSPriority(
        messageType: MessageType,
        metrics: RuntimeMetrics
    ): Int = lock.read {
        val baseQoS = messageType.defaultQoS
        val basePriority = baseQoS.priority
        
        // CRITICAL mesajlar her zaman en yüksek önceliğe sahip
        if (baseQoS == QoSLevel.CRITICAL) {
            return if (metrics.isNetworkUnstable()) {
                // Ağ kararsızsa CRITICAL önceliği artır
                120
            } else {
                100
            }
        }
        
        // ASTRA modda statik öncelikler
        if (metrics.currentMode == LifenetMode.DAILY) {
            return basePriority
        }
        
        // DISASTER modda dinamik ayarlama
        return calculateDisasterModePriority(messageType, baseQoS, basePriority, metrics)
    }
    
    /**
     * DISASTER modu için dinamik öncelik hesaplama
     */
    private fun calculateDisasterModePriority(
        messageType: MessageType,
        baseQoS: QoSLevel,
        basePriority: Int,
        metrics: RuntimeMetrics
    ): Int {
        var priority = basePriority
        
        when (baseQoS) {
            QoSLevel.BULK -> {
                // BULK mesajlar için agresif kısıtlama
                if (metrics.isBatteryCritical()) {
                    Log.d("LIFENET", "QoS: BULK stopped due to critical battery")
                    return 0  // Tamamen durdur
                }
                
                if (metrics.queueDepth > HIGH_QUEUE_DEPTH_THRESHOLD) {
                    Log.d("LIFENET", "QoS: BULK stopped due to high queue depth")
                    return 0  // Tamamen durdur
                }
                
                if (metrics.isHighLoad()) {
                    priority = 5  // Çok düşük öncelik
                }
            }
            
            QoSLevel.NORMAL -> {
                // NORMAL mesajlar için orta seviye ayarlama
                if (metrics.peerCount > HIGH_PEER_COUNT_THRESHOLD) {
                    priority = 40  // Orta kısıtlama
                }
                
                if (metrics.queueDepth > HIGH_QUEUE_DEPTH_THRESHOLD) {
                    priority = 30  // Daha fazla kısıtlama
                }
                
                if (metrics.isBatteryCritical()) {
                    priority = 35  // Batarya tasarrufu
                }
            }
            
            QoSLevel.CRITICAL -> {
                // CRITICAL için zaten handle edildi
            }
        }
        
        return priority
    }
    
    /**
     * Mesajın şu anda gönderilip gönderilmeyeceğini belirler.
     * 
     * @return true ise mesaj gönderilebilir, false ise kuyruğa alınmalı/drop edilmeli
     */
    fun shouldSendMessage(
        messageType: MessageType,
        metrics: RuntimeMetrics
    ): Boolean = lock.read {
        val priority = calculateQoSPriority(messageType, metrics)
        
        // Priority 0 ise mesaj gönderilmemeli
        if (priority == 0) {
            Log.d("LIFENET", "QoS: Message type ${messageType.name} blocked (priority=0)")
            return false
        }
        
        // CRITICAL mesajlar her zaman gönderilir
        if (messageType.defaultQoS == QoSLevel.CRITICAL) {
            return true
        }
        
        // Yüksek yük altında BULK mesajları geciktir
        if (messageType.defaultQoS == QoSLevel.BULK && metrics.isHighLoad()) {
            return false
        }
        
        return true
    }
    
    /**
     * Mesaj için gecikme süresi hesaplar (milisaniye).
     * 
     * @return Gecikme süresi (0 = hemen gönder)
     */
    fun calculateDelay(
        messageType: MessageType,
        metrics: RuntimeMetrics
    ): Long = lock.read {
        val priority = calculateQoSPriority(messageType, metrics)
        
        // CRITICAL mesajlar gecikme olmadan gönderilir
        if (messageType.defaultQoS == QoSLevel.CRITICAL) {
            return 0L
        }
        
        // Priority 0 ise sonsuz gecikme (gönderilmemeli)
        if (priority == 0) {
            return Long.MAX_VALUE
        }
        
        // ASTRA modda minimal gecikme
        if (metrics.currentMode == LifenetMode.DAILY) {
            return when (messageType.defaultQoS) {
                QoSLevel.NORMAL -> 100L
                QoSLevel.BULK -> 1000L
                else -> 0L
            }
        }
        
        // DISASTER modda dinamik gecikme
        return calculateDisasterModeDelay(messageType, priority, metrics)
    }
    
    /**
     * DISASTER modu için dinamik gecikme hesaplama
     */
    private fun calculateDisasterModeDelay(
        messageType: MessageType,
        priority: Int,
        metrics: RuntimeMetrics
    ): Long {
        return when {
            priority >= 80 -> 0L        // Yüksek öncelik: Hemen
            priority >= 50 -> 200L      // Orta öncelik: 200ms
            priority >= 30 -> 500L      // Düşük öncelik: 500ms
            priority >= 10 -> 2000L     // Çok düşük öncelik: 2s
            else -> 5000L               // Minimal öncelik: 5s
        }
    }
    
    /**
     * Mevcut runtime metrics'i toplar
     */
    fun collectRuntimeMetrics(
        peerCount: Int,
        queueDepth: Int,
        networkStabilityScore: Float
    ): RuntimeMetrics {
        return RuntimeMetrics(
            currentMode = modeManager.currentMode,
            peerCount = peerCount,
            queueDepth = queueDepth,
            batteryLevel = resourceMonitor.getBatteryLevel(),
            networkStabilityScore = networkStabilityScore
        )
    }
    
    /**
     * QoS istatistiklerini logla (debugging için)
     */
    fun logQoSStats(metrics: RuntimeMetrics) {
        Log.i("LIFENET", """
            QoS Stats:
            - Mode: ${metrics.currentMode}
            - Peers: ${metrics.peerCount}
            - Queue: ${metrics.queueDepth}
            - Battery: ${metrics.batteryLevel}%
            - Network Stability: ${metrics.networkStabilityScore}
            - High Load: ${metrics.isHighLoad()}
            - Battery Critical: ${metrics.isBatteryCritical()}
        """.trimIndent())
    }
}
