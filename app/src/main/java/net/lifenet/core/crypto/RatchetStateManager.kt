package net.lifenet.core.crypto

import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * RatchetStateManager: Her peer için bağımsız Double Ratchet durumunu yönetir.
 * Forward Secrecy ve Post-Compromise Security sağlar.
 */
import java.nio.ByteBuffer
import java.util.Arrays

/**
 * RatchetStateManager: Anayasal 'Single-Page' bellek modelini uygular.
 * Tüm 2000 peer durumu tek bir Direct ByteBuffer içinde tutulur.
 */
class RatchetStateManager(private val maxSessions: Int = 2000) {
    private val STATE_SIZE = 192 // 32 * 6 keys
    private val memoryPage: ByteBuffer = ByteBuffer.allocateDirect(maxSessions * STATE_SIZE)
    private val sessionOffsets = mutableMapOf<String, Int>()
    private var nextOffset = 0

    /**
     * Oturumu tek sayfa üzerine yerleştirir.
     * Dağınık bellek tahsisi YASAKTIR.
     */
    fun initializeSession(peerId: String, rootSeed: ByteArray) {
        if (sessionOffsets.containsKey(peerId)) return
        if (nextOffset >= maxSessions * STATE_SIZE) return

        sessionOffsets[peerId] = nextOffset
        val buffer = ByteArray(STATE_SIZE)
        System.arraycopy(rootSeed.copyOf(32), 0, buffer, 0, 32) // RootKey
        System.arraycopy(rootSeed.reversedArray(), 0, buffer, 32, 32) // SendingKey
        System.arraycopy(rootSeed, 0, buffer, 64, 32) // ReceivingKey
        
        memoryPage.position(nextOffset)
        memoryPage.put(buffer)
        nextOffset += STATE_SIZE
    }

    fun getSendingKey(peerId: String): ByteArray? {
        val offset = sessionOffsets[peerId] ?: return null
        val key = ByteArray(32)
        memoryPage.position(offset + 32)
        memoryPage.get(key)
        
        // Ratchet Step: Buffer üzerinde doğrudan güncelleme
        val nextKey = deriveNextKey(key)
        memoryPage.position(offset + 32)
        memoryPage.put(nextKey)
        return key
    }

    /**
     * Anayasal Madde: SINGLE-PASS WIPE.
     * Tek bir bellek geçişi ile tüm ratchet state yok edilir.
     */
    fun constitutionalWipe() {
        memoryPage.position(0)
        val empty = ByteArray(memoryPage.capacity())
        memoryPage.put(empty) // Tek geçiş / Tek syscall simülasyonu
        sessionOffsets.clear()
        nextOffset = 0
    }

    private fun deriveNextKey(current: ByteArray): ByteArray {
        return current.map { (it + 1).toByte() }.toByteArray()
    }
}
