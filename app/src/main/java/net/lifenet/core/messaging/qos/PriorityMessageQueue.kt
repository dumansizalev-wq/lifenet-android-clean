package net.lifenet.core.messaging.qos

import net.lifenet.core.messaging.MessageEnvelope
import java.util.PriorityQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * PriorityMessageQueue: Thread-safe öncelik bazlı mesaj kuyruğu
 * 
 * Mesajlar calculatedPriority değerine göre sıralanır (yüksekten düşüğe).
 * CRITICAL mesajlar her zaman en önde işlenir.
 */
class PriorityMessageQueue(
    private val maxSize: Int = 1000
) {
    private val queue = PriorityQueue<MessageEnvelope>(
        compareByDescending { it.calculatedPriority }
    )
    private val lock = ReentrantLock()
    
    /**
     * Kuyruğa mesaj ekler.
     * Kuyruk dolu ise en düşük öncelikli mesajı drop eder.
     */
    fun enqueue(message: MessageEnvelope): Boolean = lock.withLock {
        if (queue.size >= maxSize) {
            // Kuyruk dolu, en düşük öncelikli mesajı bul
            val lowest = queue.minByOrNull { it.calculatedPriority }
            
            // Yeni mesaj daha yüksek öncelikli ise eski mesajı çıkar
            if (lowest != null && message.calculatedPriority > lowest.calculatedPriority) {
                queue.remove(lowest)
                queue.offer(message)
                return true
            }
            return false  // Yeni mesaj eklenemedi
        }
        
        queue.offer(message)
        return true
    }
    
    /**
     * En yüksek öncelikli mesajı kuyruktan çıkarır.
     */
    fun dequeue(): MessageEnvelope? = lock.withLock {
        queue.poll()
    }
    
    /**
     * En yüksek öncelikli mesaja bakar (çıkarmadan).
     */
    fun peek(): MessageEnvelope? = lock.withLock {
        queue.peek()
    }
    
    /**
     * Kuyruk boyutunu döner.
     */
    fun size(): Int = lock.withLock {
        queue.size
    }
    
    /**
     * Kuyruğu temizler.
     */
    fun clear() = lock.withLock {
        queue.clear()
    }
    
    /**
     * Kuyruk dolu mu?
     */
    fun isFull(): Boolean = lock.withLock {
        queue.size >= maxSize
    }
    
    /**
     * Kuyruk boş mu?
     */
    fun isEmpty(): Boolean = lock.withLock {
        queue.isEmpty()
    }
}
