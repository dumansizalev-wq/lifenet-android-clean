package net.lifenet.core.security.pq

import javax.crypto.spec.SecretKeySpec

/**
 * SessionCryptoContext: Per-session ephemeral cryptographic state.
 */
class SessionCryptoContext(val peerId: String) {

    private var sessionKey: SecretKeySpec? = null
    private var lastActivity: Long = System.currentTimeMillis()

    fun establish(sharedSecret: ByteArray) {
        // Simple KDF: Take first 32 bytes of shared secret for AES-256
        val keyBytes = sharedSecret.take(32).toByteArray()
        sessionKey = SecretKeySpec(keyBytes, "AES")
        lastActivity = System.currentTimeMillis()
    }

    fun getSessionKey(): SecretKeySpec? {
        if (System.currentTimeMillis() - lastActivity > 3600000) { // 1 hour expiry
            reset()
            return null
        }
        return sessionKey
    }

    fun reset() {
        sessionKey = null
    }
}
