package net.lifenet.core.security

import android.content.Context
import android.util.Log

object TrustScoreEngine {
    private const val TAG = "TrustScoreEngine"
    
    // Reputations storage (NodeID -> Score)
    private val reputationMap = mutableMapOf<String, Int>()

    fun getTrustScore(nodeId: String): Int {
        return reputationMap.getOrDefault(nodeId, 50) // Default moderate trust
    }

    fun updateTrust(nodeId: String, relaySuccess: Boolean, validSignature: Boolean) {
        val currentScore = getTrustScore(nodeId)
        var delta = 0
        
        if (validSignature) {
            delta += 2
        } else {
            delta -= 20 // Heavy penalty for signature failure
        }
        
        if (relaySuccess) {
            delta += 1
        }
        
        val newScore = (currentScore + delta).coerceIn(0, 100)
        reputationMap[nodeId] = newScore
        
        Log.d(TAG, "Updated Trust for $nodeId: $newScore (delta: $delta)")
    }

    fun getTrustColor(score: Int): String {
        return when {
            score >= 80 -> "#00E676" // Green (Excellent)
            score >= 60 -> "#D4AF37" // Gold (Good)
            score >= 40 -> "#FFA500" // Orange (Suspicious)
            else -> "#FF0000" // Red (Toxic)
        }
    }
}
