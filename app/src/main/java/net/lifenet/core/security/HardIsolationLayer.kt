package net.lifenet.core.security

import android.content.Context
import android.util.Log
import net.lifenet.core.BuildConfig
import net.lifenet.core.messaging.MessageEnvelope

/**
 * HardIsolationLayer: Network izolasyonunun merkezi kontrol noktası
 * Build flavor'a göre otomatik olarak doğru stratejiyi seçer
 */
object HardIsolationLayer {
    
    private var strategy: NetworkIsolationStrategy? = null
    private var isInitialized = false
    
    /**
     * İzolasyon katmanını başlat
     * @param context Application context
     * @param allowedPeers Enterprise için izin verilen peer listesi (opsiyonel)
     */
    fun initialize(context: Context, allowedPeers: Set<String> = emptySet()) {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return
        }
        
        strategy = if (BuildConfig.IS_ENTERPRISE) {
            Log.i(TAG, "Initializing PRIVATE MESH isolation (Enterprise)")
            PrivateMeshIsolation(allowedPeers)
        } else {
            Log.i(TAG, "Initializing PUBLIC MESH isolation (B2C)")
            PublicMeshIsolation()
        }
        
        isInitialized = true
        Log.i(TAG, "HardIsolationLayer initialized: ${strategy?.getIsolationType()}")
    }
    
    /**
     * Peer'ın kabul edilip edilmeyeceğini kontrol et
     */
    fun canAcceptPeer(peerId: String): Boolean {
        ensureInitialized()
        return strategy?.canAcceptPeer(peerId) ?: false
    }
    
    /**
     * Mesajın relay edilip edilmeyeceğini kontrol et
     */
    fun canRelayMessage(message: MessageEnvelope): Boolean {
        ensureInitialized()
        return strategy?.canRelayMessage(message) ?: false
    }
    
    /**
     * İzin verilen peer listesini güncelle (Enterprise için)
     */
    fun updateAllowedPeers(peers: Set<String>) {
        ensureInitialized()
        strategy?.updateAllowedPeers(peers)
    }
    
    /**
     * Mevcut izolasyon türünü döndür
     */
    fun getIsolationType(): IsolationType {
        ensureInitialized()
        return strategy?.getIsolationType() ?: IsolationType.PUBLIC_MESH
    }
    
    /**
     * Enterprise için: Tek bir peer ekle
     */
    fun addAllowedPeer(peerId: String) {
        ensureInitialized()
        (strategy as? PrivateMeshIsolation)?.addAllowedPeer(peerId)
    }
    
    /**
     * Enterprise için: Tek bir peer çıkar
     */
    fun removeAllowedPeer(peerId: String) {
        ensureInitialized()
        (strategy as? PrivateMeshIsolation)?.removeAllowedPeer(peerId)
    }
    
    /**
     * Enterprise için: İzin verilen peer listesini al
     */
    fun getAllowedPeers(): Set<String> {
        ensureInitialized()
        return (strategy as? PrivateMeshIsolation)?.getAllowedPeers() ?: emptySet()
    }
    
    private fun ensureInitialized() {
        if (!isInitialized) {
            throw IllegalStateException(
                "HardIsolationLayer not initialized. Call initialize() first."
            )
        }
    }
    
    private const val TAG = "HardIsolationLayer"
}
