package net.lifenet.core.security

import net.lifenet.core.messaging.MessageEnvelope

/**
 * Network izolasyon stratejisi interface
 * B2C ve Enterprise için farklı implementasyonlar
 */
interface NetworkIsolationStrategy {
    /**
     * Peer'ın mesh ağına katılmasına izin verilir mi?
     */
    fun canAcceptPeer(peerId: String): Boolean
    
    /**
     * Mesajın relay edilmesine izin verilir mi?
     */
    fun canRelayMessage(message: MessageEnvelope): Boolean
    
    /**
     * Peer listesini güncelle (Enterprise için)
     */
    fun updateAllowedPeers(peers: Set<String>) {}
    
    /**
     * İzolasyon türünü döndür
     */
    fun getIsolationType(): IsolationType
}

enum class IsolationType {
    PUBLIC_MESH,    // B2C: Tüm peer'lar kabul edilir
    PRIVATE_MESH    // Enterprise: Sadece izin verilen peer'lar
}
