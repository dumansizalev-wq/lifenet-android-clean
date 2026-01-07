package net.lifenet.core.enterprise.kms

import android.util.Log

/**
 * Remote KMS key management strategy
 * Enterprise için uzak key yönetimi
 */
class RemoteKmsKeyManagement(
    private val kmsClient: KmsClient,
    private val keyCache: KeyCache,
    private val cacheExpirationMs: Long = 3600_000 // 1 hour
) {
    
    suspend fun storeKey(keyId: String, keyData: ByteArray) {
        try {
            // 1. KMS'e kaydet
            kmsClient.createKey(keyId, keyData)
            
            // 2. Lokal cache'e ekle
            keyCache.put(keyId, keyData, cacheExpirationMs)
            
            Log.i(TAG, "Key stored in KMS and cached: $keyId")
            
        } catch (e: KmsException) {
            Log.e(TAG, "Failed to store key in KMS", e)
            throw e
        }
    }
    
    suspend fun retrieveKey(keyId: String): ByteArray? {
        // 1. Önce cache'e bak
        keyCache.get(keyId)?.let { cachedKey ->
            Log.d(TAG, "Key retrieved from cache: $keyId")
            return cachedKey
        }
        
        // 2. Cache'de yok, KMS'ten çek
        return try {
            val keyData = kmsClient.getKey(keyId)
            
            // 3. Cache'e ekle
            keyCache.put(keyId, keyData, cacheExpirationMs)
            
            Log.i(TAG, "Key retrieved from KMS and cached: $keyId")
            keyData
            
        } catch (e: KmsException) {
            Log.e(TAG, "Failed to retrieve key from KMS", e)
            
            // 4. Graceful degradation: Süresi dolmuş cache kullan
            keyCache.getExpired(keyId)?.also {
                Log.w(TAG, "Using expired cached key (graceful degradation): $keyId")
            }
        }
    }
    
    suspend fun deleteKey(keyId: String) {
        try {
            // 1. KMS'ten sil
            kmsClient.deleteKey(keyId)
            
            // 2. Cache'den sil
            keyCache.remove(keyId)
            
            Log.i(TAG, "Key deleted from KMS and cache: $keyId")
            
        } catch (e: KmsException) {
            Log.e(TAG, "Failed to delete key from KMS", e)
            throw e
        }
    }
    
    /**
     * KMS health check
     */
    suspend fun isKmsHealthy(): Boolean {
        return kmsClient.healthCheck()
    }
    
    companion object {
        private const val TAG = "RemoteKmsKeyManagement"
    }
}
