package net.lifenet.core.security

import android.util.Log
import net.lifenet.core.messaging.MessageEnvelope

/**
 * B2C Public Mesh İzolasyon Stratejisi
 * Tüm peer'ları kabul eder, tüm mesajları relay eder
 */
class PublicMeshIsolation : NetworkIsolationStrategy {
    
    override fun canAcceptPeer(peerId: String): Boolean {
        // B2C: Tüm peer'ları kabul et
        Log.d(TAG, "Accepting peer (Public Mesh): $peerId")
        return true
    }
    
    override fun canRelayMessage(message: MessageEnvelope): Boolean {
        // B2C: Tüm mesajları relay et
        Log.d(TAG, "Relaying message (Public Mesh): ${message.id}")
        return true
    }
    
    override fun getIsolationType(): IsolationType {
        return IsolationType.PUBLIC_MESH
    }
    
    companion object {
        private const val TAG = "PublicMeshIsolation"
    }
}
