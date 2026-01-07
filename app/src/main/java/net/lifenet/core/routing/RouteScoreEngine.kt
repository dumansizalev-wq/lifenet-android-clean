package net.lifenet.core.routing

import android.util.Log

/**
 * RouteScoreEngine: Path probability and link quality assessment.
 * Calculates probability scores for potential routes.
 */
class RouteScoreEngine {

    /**
     * Path Assessment Formula:
     * Score = (RSSI_Weight * 0.4) + (SuccessRate * 0.4) - (HopPenalty * 0.2)
     */
    fun calculatePathScore(rssi: Int, successRate: Float, hops: Int): Double {
        // Normalize RSSI (Assume -100 to -30 range)
        val normalizedRssi = ((rssi + 100).toDouble() / 70.0).coerceIn(0.0, 1.0)
        
        // Hop Penalty (Linear degredation)
        val hopPenalty = (hops.toDouble() / 15.0).coerceIn(0.0, 1.0)
        
        val score = (normalizedRssi * 0.4) + (successRate * 0.4) - (hopPenalty * 0.2)
        
        return score.coerceIn(0.0, 1.0)
    }

    fun findOptimalPeer(peers: List<PeerMetric>): String? {
        return peers.maxByOrNull { calculatePathScore(it.rssi, it.successRate, it.hops) }?.peerId
    }

    data class PeerMetric(
        val peerId: String,
        val rssi: Int,
        val successRate: Float,
        val hops: Int
    )
}
