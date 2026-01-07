package net.lifenet.core.messaging

import net.lifenet.core.data.MessageType

data class MessageEnvelope(
    val id: String,
    val senderId: String,
    val targetId: String,
    val payload: ByteArray,
    val timestamp: Long,
    val hopCount: Int = 0,
    val lastHopNodeId: String = "",
    val ttl: Int = 10,
    val calculatedPriority: Int = 0,
    val messageType: MessageType = MessageType.TEXT
) {
    // Aliases for compatibility with legacy code
    val messageId: String get() = id
    val sourceId: String get() = senderId
    
    fun toBinary(): ByteArray = payload
    
    companion object {
        fun fromBinary(data: ByteArray): MessageEnvelope {
             return MessageEnvelope(
                id = java.util.UUID.randomUUID().toString(),
                senderId = "",
                targetId = "", 
                payload = data, 
                timestamp = System.currentTimeMillis()
             )
        }
    }
}
