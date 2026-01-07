package net.lifenet.core.messaging

import android.util.Log
import net.lifenet.core.contact.ContactManager
import net.lifenet.core.transport.aware.WifiAwareTransportManager

object MeshMessenger : net.lifenet.core.transport.aware.WifiAwareTransportManager.AwarenessListener {

    private var transportManager: WifiAwareTransportManager? = null
    private var myDeviceId: String = ""

    fun initialize(manager: WifiAwareTransportManager, context: android.content.Context) {
        this.transportManager = manager
        net.lifenet.core.data.MessageRepository.initialize(context)
        manager.setListener(this)
        
        // Extract my ID from Prefs (hacky access but fast)
        val prefs = context.getSharedPreferences("lifenet_identity", android.content.Context.MODE_PRIVATE)
        myDeviceId = prefs.getString("device_id_hex", "") ?: ""
    }

    fun sendMessage(toDeviceId: String, content: String) {
        val msg = net.lifenet.core.data.LifenetMessage(
            senderId = myDeviceId, // Will be me
            targetId = toDeviceId,
            content = content,
            status = net.lifenet.core.data.MessageStatus.SENT
        )
        net.lifenet.core.data.MessageRepository.addMessage(msg)
        transportManager?.publishMessage(msg)
    }
    
    // UI can observe this
    val discoveredPeers: kotlinx.coroutines.flow.StateFlow<List<String>>
        get() = transportManager?.discoveredPeers ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    
    val messageFlow = kotlinx.coroutines.flow.MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 50)
    
    // V2 Callback
    override fun onMessageReceived(senderId: String, targetId: String, msgIdHash: Long, ttl: Byte, hops: Byte, content: String) {
        // 1. Check if for me
        val isForMe = (targetId == myDeviceId)
        val isBroadcast = (targetId.all { it == '0' }) // Assume 0000... is broadcast
        
        if (isForMe || isBroadcast) {
             Log.i("MeshMessenger", "Message Received from $senderId (Hops: $hops, TTL: $ttl)")
             
             // Create Message Entity
             // Note: We don't have the original UUID, so generate a new one or use Hash if we want dedup
             val msg = net.lifenet.core.data.LifenetMessage(
                 id = msgIdHash.toString(), // Using hash as ID for receiver
                 senderId = senderId,
                 targetId = targetId,
                 content = content,
                 ttl = ttl.toInt(),
                 hops = hops.toInt(),
                 status = net.lifenet.core.data.MessageStatus.DELIVERED
             )
             
             // Avoid duplicate add (simple check)
             val exists = net.lifenet.core.data.MessageRepository.getMessagesForDevice(senderId).any { it.content == content && it.timestamp > System.currentTimeMillis() - 10000 }
             if (!exists) {
                 net.lifenet.core.data.MessageRepository.addMessage(msg)
             }
        } else {
            Log.d("MeshMessenger", "Ignoring message for $targetId")
        }
    }
    
    // VSIE Binding
    fun bindVsieManager(vsieManager: net.lifenet.core.transport.vsie.VsieManager, scope: kotlinx.coroutines.CoroutineScope) {
        scope.launch {
            vsieManager.discoveredMessages.collect { vsieMessages ->
                vsieMessages.forEach { msg ->
                    // Convert VsieMessage to LifenetMessage and Persist
                    val contentStr = String(msg.payload, Charsets.UTF_8)
                    
                    // Basic deduplication check
                    val exists = net.lifenet.core.data.MessageRepository.getAllMessages().any { 
                        it.content == contentStr && it.senderId == msg.senderId 
                    }
                    
                    if (!exists) {
                         val lifenetMsg = net.lifenet.core.data.LifenetMessage(
                             id = msg.id,
                             senderId = msg.senderId,
                             targetId = "BROADCAST", // Assume broadcast for VSIE unless header parsed deeper
                             content = contentStr,
                             ttl = msg.ttl,
                             hops = msg.hops,
                             status = net.lifenet.core.data.MessageStatus.DELIVERED
                         )
                         net.lifenet.core.data.MessageRepository.addMessage(lifenetMsg)
                         Log.i("MeshMessenger", "Persisted VSIE Message from ${msg.senderId}")
                    }
                }
            }
        }
    }
}
