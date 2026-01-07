package net.lifenet.core.routing

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * PredictiveRoutingEngine: Jitter ve gecikme verilerini kullanarak "Zehirli" (jammer altındaki)
 * düğümleri tespit eder ve trafiği otonom olarak yeniden yönlendirir.
 */
class PredictiveRoutingEngine {

    private val nodeHeartbeatHistory = ConcurrentHashMap<String, MutableList<Long>>()
    private val toxicNodes = mutableSetOf<String>()

    private val JITTER_THRESHOLD_MS = 150L
    private val HISTORY_SIZE = 10

    /**
     * Düğümden gelen gecikme verisini kaydeder ve "Zehirlilik" analizi yapar.
     */
    fun recordLatency(peerId: String, latency: Long) {
        val history = nodeHeartbeatHistory.getOrPut(peerId) { mutableListOf() }
        history.add(latency)
        if (history.size > HISTORY_SIZE) history.removeAt(0)

        analyzeNodeHealth(peerId, history)
    }

    private fun analyzeNodeHealth(peerId: String, history: List<Long>) {
        if (history.size < 5) return

        // Jitter Hesaplama (Gecikme değişimi)
        val jitter = calculateJitter(history)

        if (jitter > JITTER_THRESHOLD_MS) {
            if (!toxicNodes.contains(peerId)) {
                Log.w("LIFENET", "PredictiveRouting: Node $peerId detected as TOXIC (Jitter: ${jitter}ms). Rerouting...")
                toxicNodes.add(peerId)
                // MetricCollector çağrısı stub ile değiştirildi
                MetricCollector.handlePortalMessage()
                // Gerçek implementasyonda PacketRouter'a kara liste bildirilir.
            }
        } else if (toxicNodes.contains(peerId) && jitter < JITTER_THRESHOLD_MS / 2) {
            Log.i("LIFENET", "PredictiveRouting: Node $peerId recovered. Removing from toxic set.")
            toxicNodes.remove(peerId)
        }
    }

    private fun calculateJitter(history: List<Long>): Double {
        var totalDiff = 0L
        for (i in 1 until history.size) {
            totalDiff += kotlin.math.abs(history[i] - history[i - 1])
        }
        return totalDiff.toDouble() / (history.size - 1)
    }

    /**
     * Düğümün güvenilir olup olmadığını döner.
     */
    fun isNodeReliable(peerId: String): Boolean {
        return !toxicNodes.contains(peerId)
    }

    fun getToxicNodesCount(): Int = toxicNodes.size
}

/**
 * MetricCollector stub
 * handlePortalMessage fonksiyonu implementasyon öncesi stub olarak tanımlandı
 */
object MetricCollector {
    fun handlePortalMessage() {
        // TODO: Gerçek metrik işlevi implement edilecek
    }
}
