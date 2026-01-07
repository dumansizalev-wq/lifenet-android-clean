package net.lifenet.core.enterprise.kms

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory key cache with expiration
 * Graceful degradation için KMS erişilemediğinde kullanılır
 */
class KeyCache {
    
    private val cache = ConcurrentHashMap<String, CachedKey>()
    
    /**
     * Key'i cache'e ekle
     */
    fun put(keyId: String, keyData: ByteArray, expirationMs: Long) {
        val expiresAt = System.currentTimeMillis() + expirationMs
        cache[keyId] = CachedKey(keyData, expiresAt, false)
        Log.d(TAG, "Key cached: $keyId (expires in ${expirationMs}ms)")
    }
    
    /**
     * Cache'den key al (sadece geçerli key'ler)
     */
    fun get(keyId: String): ByteArray? {
        val cached = cache[keyId] ?: return null
        
        return if (cached.isExpired()) {
            Log.d(TAG, "Key expired: $keyId")
            // Expired key'i işaretle ama silme (graceful degradation için)
            cache[keyId] = cached.copy(expired = true)
            null
        } else {
            Log.d(TAG, "Key retrieved from cache: $keyId")
            cached.keyData
        }
    }
    
    /**
     * Süresi dolmuş key'i al (graceful degradation)
     * KMS erişilemediğinde kullanılır
     */
    fun getExpired(keyId: String): ByteArray? {
        val cached = cache[keyId]
        return if (cached != null && cached.expired) {
            Log.w(TAG, "Using EXPIRED key (graceful degradation): $keyId")
            cached.keyData
        } else {
            null
        }
    }
    
    /**
     * Key'i cache'den sil
     */
    fun remove(keyId: String) {
        cache.remove(keyId)
        Log.d(TAG, "Key removed from cache: $keyId")
    }
    
    /**
     * Tüm cache'i temizle
     */
    fun clear() {
        cache.clear()
        Log.d(TAG, "Cache cleared")
    }
    
    /**
     * Süresi dolmuş key'leri temizle
     */
    fun evictExpired() {
        val expiredKeys = cache.filter { it.value.isExpired() }.keys
        expiredKeys.forEach { cache.remove(it) }
        
        if (expiredKeys.isNotEmpty()) {
            Log.d(TAG, "Evicted ${expiredKeys.size} expired keys")
        }
    }
    
    private data class CachedKey(
        val keyData: ByteArray,
        val expiresAt: Long,
        val expired: Boolean = false
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as CachedKey
            if (!keyData.contentEquals(other.keyData)) return false
            if (expiresAt != other.expiresAt) return false
            if (expired != other.expired) return false
            return true
        }

        override fun hashCode(): Int {
            var result = keyData.contentHashCode()
            result = 31 * result + expiresAt.hashCode()
            result = 31 * result + expired.hashCode()
            return result
        }
    }
    
    companion object {
        private const val TAG = "KeyCache"
    }
}
