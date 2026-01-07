package net.lifenet.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

import net.lifenet.core.messaging.qos.QoSLevel

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val timestamp: Long,
    val messageType: MessageType = MessageType.TEXT,
    val mediaPayload: String? = null, // Base64 for voice/image
    val isEncrypted: Boolean = true,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.PENDING,
    val isSentByMe: Boolean = false,
    val hopCount: Int = 0,
    val lastHopNodeId: String? = null,
    val channelId: String? = null // For group broadcasts
)


enum class MessageType(val defaultQoS: QoSLevel) {
    // CRITICAL
    PTT(QoSLevel.CRITICAL),
    SOS(QoSLevel.CRITICAL),
    SYSTEM(QoSLevel.CRITICAL),
    HEARTBEAT(QoSLevel.CRITICAL),
    
    // NORMAL
    TEXT(QoSLevel.NORMAL),
    LOCATION(QoSLevel.NORMAL),
    NOTIFICATION(QoSLevel.NORMAL),
    IMAGE(QoSLevel.NORMAL),
    
    // BULK
    LOG(QoSLevel.BULK),
    SYNC(QoSLevel.BULK),
    FILE(QoSLevel.BULK),
    TELEMETRY(QoSLevel.BULK)
}

enum class DeliveryStatus {
    PENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
