package net.lifenet.core.mesh.engine

import net.lifenet.core.routing.RouteScoreEngine

/**
 * GatewayNodeSelector: Cluster'lar arası (inter-cluster) iletişim için geçit düğümlerini seçer.
 * Sadece SOS (0x01) ve Astra-Heartbeat (0x02) paketlerinin geçişine izin verir.
 */
class GatewayNodeSelector(
    private val nodeId: String,
    private val routeScoreEngine: RouteScoreEngine
) {
    private var confidenceScore: Float = 0f
    private var consistentPacketCount: Int = 0
    private var isGatewayActive: Boolean = false

    /**
     * Düğümün Gateway rolünü üstlenip üstlenmeyeceğine karar verir.
     * Dinamik Confidence Score ve anayasal hız sınırlamaları uygulanır.
     */
    fun evaluateGatewayStatus(recentPackets: Int, congestionScore: Int): Boolean {
        // Dinamik Eşik: Yoğunluk arttıkça güven eşiği düşürülür (kaos ortamı adaptasyonu)
        val threshold = if (congestionScore > 85) 0.6f else 0.8f
        
        // Confidence Score Hesaplama (Basitleştirilmiş)
        confidenceScore = (recentPackets.toFloat() / 100f).coerceIn(0f, 1f)

        // Anayasal Madde: İlk 10 ardışık tutarlı paket ve eşik kontrolü
        if (consistentPacketCount >= 10 && confidenceScore >= threshold) {
            isGatewayActive = true
        } else {
            consistentPacketCount++
        }

        return isGatewayActive
    }

    /**
     * Inter-cluster trafik için paket tipini doğrular.
     * SOS (0x01) her zaman önceliklidir ve bekletilmez.
     */
    fun isAllowedInterCluster(packetType: Byte): Boolean {
        return packetType == 0x01.toByte() || packetType == 0x02.toByte()
    }
}
