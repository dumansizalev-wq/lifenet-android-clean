package net.lifenet.core.messaging.assembler

import java.nio.ByteBuffer

/**
 * ForwardErrorCorrection: Kaybolan paketleri kurtarmak için XOR tabanlı yedeklilik sağlar.
 */
class ForwardErrorCorrection {

    /**
     * Verilen fragmanlar için XOR parity chunk üretir.
     */
    fun generateParity(messageId: Long, fragments: List<ByteArray>): ByteArray {
        if (fragments.isEmpty()) return ByteArray(0)
        
        val maxLen = fragments.maxOf { it.size }
        val parity = ByteArray(maxLen)

        for (fragment in fragments) {
            for (i in fragment.indices) {
                parity[i] = (parity[i].toInt() xor fragment[i].toInt()).toByte()
            }
        }

        // FEC Header: [MessageID(8)|TotalChunks(2)|ChunkIndex(2)|IsFEC(1)|Checksum(4)]
        // Parity chunk her zaman son index + 1 olarak işaretlenir.
        return parity
    }

    /**
     * Eksik olan tek bir fragmanı XOR üzerinden kurtarır.
     */
    fun recover(fragments: List<ByteArray>, parity: ByteArray): ByteArray {
        val recovered = parity.copyOf()
        for (fragment in fragments) {
            for (i in fragment.indices) {
                recovered[i] = (recovered[i].toInt() xor fragment[i].toInt()).toByte()
            }
        }
        return recovered
    }
}
