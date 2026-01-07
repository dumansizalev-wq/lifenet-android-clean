package net.lifenet.core.security

import android.util.Log
import net.lifenet.core.messaging.MessageEnvelope
import java.util.concurrent.ConcurrentHashMap

/**
 * Enterprise Private Mesh İzolasyon Stratejisi
 * Sadece izin verilen peer'ları kabul eder
 */
class PrivateMeshIsolation(
    initialAllowedPeers: Set<String> = emptySet()
) : NetworkIsolationStrategy {
    
    // Thread-safe allowed peers listesi
    private val allowedPeers = ConcurrentHashMap<String, Boolean>()
    
    init {
        initialAllowedPeers.forEach { peerId ->
            allowedPeers[peerId] = true
        }
        Log.i(TAG, "Initialized with ${allowedPeers.size} allowed peers")
    }
    
    override fun canAcceptPeer(peerId: String): Boolean {
        val allowed = allowedPeers.containsKey(peerId)
        
        if (allowed) {
            Log.d(TAG, "Accepting peer (Private Mesh): $peerId")
        } else {
            Log.w(TAG, "Rejecting peer (Private Mesh): $peerId - NOT in allowed list")
        }
        
        return allowed
    }
    
    override fun canRelayMessage(message: MessageEnvelope): Boolean {
        // Mesajın hem göndereni hem de alıcısı izin verilen listede olmalı
        val senderAllowed = allowedPeers.containsKey(message.senderId)
        val targetAllowed = message.targetId == "BROADCAST" || 
                           allowedPeers.containsKey(message.targetId)
        
        val canRelay = senderAllowed && targetAllowed
        
        if (canRelay) {
            Log.d(TAG, "Relaying message (Private Mesh): ${message.id}")
        } else {
            Log.w(TAG, "Blocking message (Private Mesh): ${message.id} - " +
                      "Sender: $senderAllowed, Target: $targetAllowed")
        }
        
        return canRelay
    }
    
    override fun updateAllowedPeers(peers: Set<String>) {
        allowedPeers.clear()
        peers.forEach { peerId ->
            allowedPeers[peerId] = true
        }
        Log.i(TAG, "Updated allowed peers: ${allowedPeers.size} peers")
    }
    
    override fun getIsolationType(): IsolationType {
        return IsolationType.PRIVATE_MESH
    }
    
    /**
     * Allowed peers listesini döndür (read-only)
     */
    fun getAllowedPeers(): Set<String> {
        return allowedPeers.keys.toSet()
    }
    
    /**
     * Tek bir peer ekle
     */
    fun addAllowedPeer(peerId: String) {
        allowedPeers[peerId] = true
        Log.i(TAG, "Added allowed peer: $peerId")
    }
    
    /**
     * Tek bir peer çıkar
     */
    fun removeAllowedPeer(peerId: String) {
        allowedPeers.remove(peerId)
        Log.i(TAG, "Removed allowed peer: $peerId")
    }
    
    companion object {
        private const val TAG = "PrivateMeshIsolation"
    }
}
