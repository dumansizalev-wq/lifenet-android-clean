package net.lifenet.core.security.identity

import java.nio.ByteBuffer
import java.security.Signature
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

/**
 * TrustAssertion: Provides proof of authenticity for mesh signaling.
 */
class TrustAssertion(private val identity: NodeIdentity) {

    fun createAssertion(nonce: Long): ByteArray {
        val dataToSign = ByteBuffer.allocate(8).putLong(nonce).array()
        val signature = identity.signData(dataToSign)
        val publicKey = identity.getPublicKey()

        // Assertion = [PubKeySize(4)|PubKey|SigSize(4)|Signature]
        return ByteBuffer.allocate(4 + publicKey.size + 4 + signature.size)
            .putInt(publicKey.size)
            .put(publicKey)
            .putInt(signature.size)
            .put(signature)
            .array()
    }

    companion object {
        fun verifyAssertion(assertion: ByteArray, nonce: Long): Boolean {
            try {
                val buffer = ByteBuffer.wrap(assertion)
                val pubKeySize = buffer.getInt()
                val pubKeyBytes = ByteArray(pubKeySize)
                buffer.get(pubKeyBytes)
                val sigSize = buffer.getInt()
                val sigBytes = ByteArray(sigSize)
                buffer.get(sigBytes)

                val keyFactory = KeyFactory.getInstance("EC")
                val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(pubKeyBytes))

                val signature = Signature.getInstance("SHA256withECDSA").apply {
                    initVerify(publicKey)
                    update(ByteBuffer.allocate(8).putLong(nonce).array())
                }
                return signature.verify(sigBytes)
            } catch (e: Exception) {
                return false
            }
        }
    }
}
