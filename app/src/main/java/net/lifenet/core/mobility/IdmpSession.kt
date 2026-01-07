package net.lifenet.core.mobility

import android.util.Log
import java.util.UUID

/**
 * IdmpSession: Intra-Domain Mobility Protocol session state.
 * Maintains continuity when node properties (IP/MAC) change.
 */
class IdmpSession(val nodeId: String) {

    var sessionId: String = UUID.randomUUID().toString()
    var currentTransport: String = "INITIAL"
    var lastKnownAddress: String = "00:00:00:00:00:00"
    var startTime: Long = System.currentTimeMillis()
    var isActive: Boolean = true

    fun updateAnchor(newAddress: String, transport: String) {
        if (newAddress != lastKnownAddress || transport != currentTransport) {
            Log.i("LIFENET", "IDMP: Anchor updated. Transport: $currentTransport -> $transport")
            this.lastKnownAddress = newAddress
            this.currentTransport = transport
        }
    }

    fun validateSession(): Boolean {
        // Cleanup logic if session is too old without update
        val sessionAge = System.currentTimeMillis() - startTime
        if (sessionAge > 3600000) { // 1 hour
            isActive = false
        }
        return isActive
    }
}
