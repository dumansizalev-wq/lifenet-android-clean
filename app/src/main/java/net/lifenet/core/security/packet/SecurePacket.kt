package net.lifenet.core.security.packet

import android.util.Log
import net.lifenet.core.security.identity.NodeIdentity
import net.lifenet.core.security.identity.TrustAssertion
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * SecurePacket: Immutable, authenticated, and encrypted envelope.
 */
class SecurePacket(private val identity: NodeIdentity) {

    private val assertionHelper = TrustAssertion(identity)

    /**
     * Wrap: [AssertionSize(4)|Assertion|Nonce(8)|EncryptedPayload]
     */
    fun wrap(payload: ByteArray, sessionKey: SecretKeySpec, nonce: Long): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteBuffer.allocate(12).putLong(nonce).put(byteArrayOf(0, 0, 0, 0)).array()
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey, GCMParameterSpec(128, iv))
        
        val encrypted = cipher.doFinal(payload)
        val assertion = assertionHelper.createAssertion(nonce)

        return ByteBuffer.allocate(4 + assertion.size + 8 + encrypted.size)
            .putInt(assertion.size)
            .put(assertion)
            .putLong(nonce)
            .put(encrypted)
            .array()
    }

    fun unwrap(secureData: ByteArray, sessionKey: SecretKeySpec): ByteArray? {
        try {
            val buffer = ByteBuffer.wrap(secureData)
            val assertionSize = buffer.getInt()
            val assertion = ByteArray(assertionSize)
            buffer.get(assertion)
            val nonce = buffer.getLong()
            
            // 1. Verify Trust Assertion
            if (!TrustAssertion.verifyAssertion(assertion, nonce)) {
                Log.e("LIFENET", "Security: Untrusted packet signature! DROP.")
                return null
            }

            // 2. Decrypt
            val encrypted = ByteArray(buffer.remaining())
            buffer.get(encrypted)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteBuffer.allocate(12).putLong(nonce).put(byteArrayOf(0, 0, 0, 0)).array()
            cipher.init(Cipher.DECRYPT_MODE, sessionKey, GCMParameterSpec(128, iv))
            
            return cipher.doFinal(encrypted)
        } catch (e: Exception) {
            Log.e("LIFENET", "Security: Packet decryption failed: ${e.message}")
            return null
        }
    }
}
