package net.lifenet.core.security.pq

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import javax.crypto.KeyAgreement

/**
 * PqKeyManager: Hybrid Key Exchange (ECC + PQ Hooks).
 * Handles ephemeral key generation for session establishment.
 */
class PqKeyManager {

    fun generateEphemeralKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(256)
        return kpg.generateKeyPair()
    }

    /**
     * computeSharedSecret: Standard Diffie-Hellman hook.
     * Hybridization with PQ (e.g. Kyber) occurs by concatenating ECC secret 
     * with PQ shared secret before KDF.
     */
    fun computeSharedSecret(localPrivate: java.security.PrivateKey, remotePublic: java.security.PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(localPrivate)
        ka.doPhase(remotePublic, true)
        return ka.generateSecret()
    }
    
    fun generateRandomNonce(): Long {
        return SecureRandom().nextLong()
    }
}
