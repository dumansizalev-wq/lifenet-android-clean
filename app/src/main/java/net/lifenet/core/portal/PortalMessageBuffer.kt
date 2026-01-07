package net.lifenet.core.portal

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * PortalMessageBuffer: Portal üzerinden gelen mesajları mesh'e aktarılana kadar tutar.
 */
class PortalMessageBuffer {
    
    data class PortalMessage(
        val id: String = UUID.randomUUID().toString(),
        val content: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val queue = ConcurrentLinkedQueue<PortalMessage>()

    fun enqueue(content: String) {
        queue.add(PortalMessage(content = content))
    }

    fun poll(): PortalMessage? = queue.poll()

    fun isEmpty(): Boolean = queue.isEmpty()

    /**
     * Anayasal Temizlik: Tüm buffer'ı tek geçişte siler.
     */
    fun clear() {
        queue.clear()
    }
}
