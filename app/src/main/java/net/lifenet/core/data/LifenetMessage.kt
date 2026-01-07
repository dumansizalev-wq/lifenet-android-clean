package net.lifenet.core.data

import java.util.UUID

enum class MessageStatus {
    PENDING, SENT, DELIVERED, FAILED
}

data class LifenetMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val targetId: String, // "BROADCAST" for broadcast
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 3,
    val hops: Int = 0,
    var status: MessageStatus = MessageStatus.PENDING
)
