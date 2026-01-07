package net.lifenet.core.crypto

import java.util.*
import javax.crypto.SecretKey

/**
 * ForwardSecrecyEnforcer: Eski anahtarların imhasını ve ratchet sürekliliğini sağlar.
 */
class ForwardSecrecyEnforcer {

    /**
     * Kullanılmış anahtar materyalini bellekten güvenli bir şekilde temizler.
     */
    fun secureWipe(key: ByteArray) {
        Arrays.fill(key, 0.toByte())
    }

    /**
     * Bir oturumun ratchet adımını zorunlu kılar.
     * Eğer bir anahtar belirli bir süre/kullanım limitini aşarsa yenilenmelidir.
     */
    fun isRatchetRequired(messageCount: Int, lastRotation: Long): Boolean {
        val MAX_MESSAGES_PER_KEY = 50
        val MAX_TIME_MS = 3600000L // 1 Saat
        
        return messageCount >= MAX_MESSAGES_PER_KEY || 
               (System.currentTimeMillis() - lastRotation) > MAX_TIME_MS
    }
}
