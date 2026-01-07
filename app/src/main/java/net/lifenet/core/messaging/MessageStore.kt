package net.lifenet.core.messaging

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Log
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import net.lifenet.core.data.MessageType

/**
 * MessageStore
 * Encrypted FIFO persistence with CONSTITUTIONAL PHYSICAL WIPE.
 */
class MessageStore(private val context: Context) {

    private val storeDir: File = File(context.filesDir, "msgstore").apply { mkdirs() }
    private val rng = SecureRandom()
    private val queue = ConcurrentLinkedQueue<String>() // Message IDs
    private val MAX_QUEUE_SIZE = 1000

    fun currentSize(): Int = queue.size
    fun maxSize(): Int = MAX_QUEUE_SIZE

    // --- Encryption (AES-256-GCM) ---
    private val key: SecretKey = SecretKeySpec(loadOrCreateKey(), "AES")

    init {
        // Reload queue from disk
        storeDir.listFiles()?.filter { it.name.endsWith(".msg") }?.forEach { file ->
            val id = file.name.removeSuffix(".msg")
            if (id.isNotEmpty()) queue.add(id)
        }
    }

    fun push(envelope: MessageEnvelope) {
        if (queue.size >= MAX_QUEUE_SIZE) {
            val oldestId = queue.poll()
            if (oldestId != null) delete(oldestId)
        }
        queue.add(envelope.messageId)
        put(envelope.messageId.toString(), envelope.payload)
    }

    fun pollAll(): List<MessageEnvelope> {
        val result = mutableListOf<MessageEnvelope>()
        queue.forEach { id ->
            val bytes = get(id)
            if (bytes != null) {
                // Burada MessageEnvelope oluştururken doğru parametreleri gönderiyoruz.
                // Eğer hata devam ederse MessageEnvelope.kt dosyasındaki sıralamaya göre
                // bu parametrelerin yerini değiştirin.
                result.add(
                    MessageEnvelope(
                        id = id,
                        senderId = "RECONSTRUCTED",
                        targetId = "RECONSTRUCTED",
                        lastHopNodeId = "RECONSTRUCTED",
                        ttl = 10,
                        hopCount = 0,
                        payload = bytes,
                        timestamp = System.currentTimeMillis(),
                        messageType = net.lifenet.core.data.MessageType.TEXT
                    )
                )
            }
        }
        return result
    }

    fun put(id: String, plaintext: ByteArray) {
        val file = fileFor(id)
        val iv = ByteArray(12).also { rng.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
        val ciphertext = cipher.doFinal(plaintext)
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.writeInt(iv.size)
            raf.write(iv)
            raf.write(ciphertext)
            raf.fd.sync()
        }
    }

    fun get(id: String): ByteArray? {
        val file = fileFor(id)
        if (!file.exists()) return null
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val ivLen = raf.readInt()
                val iv = ByteArray(ivLen).also { raf.readFully(it) }
                val ciphertext = ByteArray((raf.length() - 4 - ivLen).toInt()).also { raf.readFully(it) }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
                }
                cipher.doFinal(ciphertext)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun delete(id: String) {
        val file = fileFor(id)
        if (!file.exists()) return
        physicalWipe(file, passes = 1)
        file.delete()
        queue.remove(id)
    }

    fun expire(ids: List<String>) {
        ids.forEach { delete(it) }
    }

    private fun physicalWipe(file: File, passes: Int) {
        RandomAccessFile(file, "rw").use { raf ->
            val length = raf.length()
            if (length <= 0) {
                raf.fd.sync()
                return
            }
            val buffer = ByteArray(64 * 1024)
            repeat(passes) {
                raf.seek(0)
                var remaining = length
                while (remaining > 0) {
                    rng.nextBytes(buffer)
                    val toWrite = minOf(buffer.size.toLong(), remaining).toInt()
                    raf.write(buffer, 0, toWrite)
                    remaining -= toWrite
                }
                raf.fd.sync()
            }
            raf.setLength(0)
            raf.fd.sync()
        }
    }

    private fun fileFor(id: String): File = File(storeDir, "$id.msg")

    private fun loadOrCreateKey(): ByteArray {
        val keyFile = File(context.filesDir, ".msg_master.key")
        return if (keyFile.exists()) {
            keyFile.readBytes()
        } else {
            val k = ByteArray(32).also { rng.nextBytes(it) }
            keyFile.writeBytes(k)
            k
        }
    }
}

// DİKKAT: Alttaki MessageEnvelope tanımını sildim çünkü MessageEnvelope.kt dosyasında zaten var.
// Eğer MessageEnvelope.kt dosyası projenizde YOKSA, bu satırın altına tekrar ekleyebilirsiniz.
