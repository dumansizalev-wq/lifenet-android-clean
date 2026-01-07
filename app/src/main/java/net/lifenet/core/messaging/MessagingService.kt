package net.lifenet.core.messaging

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.lifenet.core.data.DeliveryStatus
import net.lifenet.core.data.LifenetDatabase
import net.lifenet.core.data.Message
import net.lifenet.core.mesh.GhostRadioService
import android.util.Log 
import net.lifenet.core.messaging.MessageEnvelope
import net.lifenet.core.mode.LifenetMode
import net.lifenet.core.mode.ModeManager

// Service to handle messaging operations
class LifenetMessagingService(private val context: Context) {
    
    private val database = LifenetDatabase.getInstance(context)
    private val messageDao = database.messageDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val identityManager = net.lifenet.core.identity.IdentityManager(context)
    private val modeManager = ModeManager.getInstance(context)
    private val subscribedChannels = mutableSetOf<String>("GENERAL", "SYSTEM")
    
    fun getMessages(): kotlinx.coroutines.flow.Flow<List<Message>> {
        return messageDao.getAllMessages()
    }
    
    fun sendMessage(
        recipientId: String, 
        content: String, 
        type: net.lifenet.core.data.MessageType = net.lifenet.core.data.MessageType.TEXT,
        mediaPayload: String? = null,
        channelId: String? = null,
        onSuccess: () -> Unit = {}, 
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            try {
                // E2EE: Encrypt content if it's sensitive
                val encryptedContent = if (type == net.lifenet.core.data.MessageType.TEXT) {
                    identityManager.encryptPayload(content, recipientId)
                } else {
                    content
                }

                val message = Message(
                    senderId = getOwnNodeId(),
                    recipientId = recipientId,
                    content = encryptedContent,
                    timestamp = System.currentTimeMillis(),
                    messageType = type,
                    mediaPayload = mediaPayload,
                    isEncrypted = true,
                    deliveryStatus = DeliveryStatus.PENDING,
                    isSentByMe = true,
                    channelId = channelId
                )
                
                // Save to database
                val messageId = messageDao.insertMessage(message)
                
                // Route message based on mode
                when (modeManager.currentMode) {
                    LifenetMode.DAILY -> sendViaTor(message)
                    else -> sendViaMesh(message)
                }
                
                // Update status
                messageDao.updateDeliveryStatus(messageId, DeliveryStatus.SENT)
                onSuccess()
                
            } catch (e: Exception) {
                onError(e.message ?: "Failed to send message")
            }
        }
    }
    
    private fun sendViaTor(message: Message) {
        // Structured payload: TYPE|SENDER|CONTENT|MEDIA
        val payloadStr = "${message.messageType.name}|${message.senderId}|${message.content}|${message.mediaPayload ?: ""}"
        val payload = payloadStr.toByteArray()
        // TODO: Pass to Tor Service
    }
    
    private fun sendViaMesh(message: Message) {
        // Structured payload: TYPE|SENDER|CONTENT|MEDIA
        val payloadStr = "${message.messageType.name}|${message.senderId}|${message.content}|${message.mediaPayload ?: ""}"
        val rawPayload = payloadStr.toByteArray()
        val compressedPayload = net.lifenet.core.utils.CompressionManager.compress(rawPayload)
        
        // TODO: Create MessageEnvelope and pass to GhostRadioService
        Log.i("MessagingService", "Queued mesh message (compressed: ${compressedPayload.size} bytes)")
    }
    
    private fun getOwnNodeId(): String {
        return identityManager.getCurrentNodeId()
    }

    /**
     * Mesajın mesh katmanından (GhostRadioService -> RoutingEngine) gelişi.
     */
    fun handleReceivedMeshMessage(envelope: MessageEnvelope) {
        scope.launch {
            try {
                // Decompress payload
                val decompressedPayload = net.lifenet.core.utils.CompressionManager.decompress(envelope.payload)
                
                // Structured payload: TYPE|SENDER|CONTENT|MEDIA
                val payloadStr = String(decompressedPayload)
                val parts = payloadStr.split("|")
                if (parts.size < 3) return@launch

                val typeStr = parts[0]
                val senderId = parts[1]
                val content = parts[2]
                val mediaPayload = if (parts.size > 3) parts[3] else null

                val messageType = try {
                    net.lifenet.core.data.MessageType.valueOf(typeStr)
                } catch (e: Exception) {
                    net.lifenet.core.data.MessageType.TEXT
                }

                // Create message record
                val message = Message(
                    senderId = senderId,
                    recipientId = envelope.targetId,
                    content = content, // Will be decrypted on-the-fly in UI if marked as encrypted
                    timestamp = envelope.timestamp,
                    messageType = messageType,
                    mediaPayload = mediaPayload,
                    isEncrypted = true, // Force E2EE marker for mesh messages
                    deliveryStatus = DeliveryStatus.DELIVERED,
                    isSentByMe = false,
                    hopCount = envelope.hopCount,
                    lastHopNodeId = envelope.lastHopNodeId,
                    channelId = if (envelope.targetId.startsWith("BROADCAST:")) envelope.targetId.substring(10) else null
                )

                // Only save if it's for me OR a channel I'm subscribed to
                val targetChannel = message.channelId
                if (envelope.targetId == "BROADCAST" || 
                    envelope.targetId == getOwnNodeId() ||
                    (targetChannel != null && subscribedChannels.contains(targetChannel))) {
                    
                    messageDao.insertMessage(message)
                    Log.i("MessagingService", "Received and stored mesh message from $senderId ${if (targetChannel != null) "in #$targetChannel" else ""}")
                }

            } catch (e: Exception) {
                Log.e("MessagingService", "Error parsing received mesh message", e)
            }
        }
    }
}
