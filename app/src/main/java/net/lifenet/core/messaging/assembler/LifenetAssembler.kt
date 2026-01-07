package net.lifenet.core.messaging.assembler

import android.util.Log
import net.lifenet.core.mesh.engine.MetricCollector
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * LifenetAssembler: Parçalanmış paketleri birleştirir ve eksik olanları FEC ile kurtarır.
 */
class LifenetAssembler(private val fecEngine: ForwardErrorCorrection) {

    private val sessionCache = ConcurrentHashMap<Long, AssemblySession>()
    private val SESSION_TIMEOUT_MS = 300000L // 5 Dakika (Ghost-Deletion uyumlu)

    data class AssemblySession(
        val messageId: Long,
        val totalChunks: Int,
        val fragments: MutableMap<Int, ByteArray> = mutableMapOf(),
        var parityChunk: ByteArray? = null,
        val startTime: Long = System.currentTimeMillis()
    )

    /**
     * Gelen fragmanı işler. Eğer mesaj tamamlanırsa birleştirilmiş hali döner.
     */
    fun onFragmentReceived(
        messageId: Long,
        totalChunks: Int,
        chunkIndex: Int,
        isFEC: Boolean,
        data: ByteArray
    ): ByteArray? {
        MetricCollector.incrementTotalFragments()
        val session = sessionCache.getOrPut(messageId) { 
            AssemblySession(messageId, totalChunks) 
        }

        if (isFEC) {
            session.parityChunk = data
        } else {
            session.fragments[chunkIndex] = data
        }

        // Temizlik: Eskimiş oturumları at
        cleanExpiredSessions()

        // 1. Tamamlanma kontrolü
        if (session.fragments.size == totalChunks) {
            return finalizeSession(messageId)
        }

        // 2. FEC Recovery kontrolü (Sadece 1 paket eksikse ve Parity elimizdeyse)
        if (session.fragments.size == totalChunks - 1 && session.parityChunk != null) {
            Log.i("LIFENET", "Assembler: Triggering FEC Recovery for Message $messageId")
            MetricCollector.incrementFecRecovery()
            val missingIndex = (0 until totalChunks).first { !session.fragments.containsKey(it) }
            val recovered = fecEngine.recover(session.fragments.values.toList(), session.parityChunk!!)
            session.fragments[missingIndex] = recovered
            return finalizeSession(messageId)
        }

        return null
    }

    private fun finalizeSession(messageId: Long): ByteArray? {
        val session = sessionCache.remove(messageId) ?: return null
        val totalSize = session.fragments.values.sumOf { it.size }
        val finalBuffer = ByteBuffer.allocate(totalSize)
        
        for (i in 0 until session.totalChunks) {
            finalBuffer.put(session.fragments[i])
        }
        
        Log.i("LIFENET", "Assembler: Message $messageId reassembled successfully (${totalSize} bytes)")
        return finalBuffer.array()
    }

    private fun cleanExpiredSessions() {
        val now = System.currentTimeMillis()
        sessionCache.entries.removeIf { now - it.value.startTime > SESSION_TIMEOUT_MS }
    }
}
