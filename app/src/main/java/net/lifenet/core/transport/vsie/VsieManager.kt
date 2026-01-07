package net.lifenet.core.transport.vsie

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.aware.*
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import kotlin.collections.HashMap

data class VsieMessage(
    val id: String,
    val payload: ByteArray,
    var ttl: Int = 5,
    var hops: Int = 0,
    val senderId: String
)

@SuppressLint("MissingPermission")
class VsieManager(private val context: Context) {

    private val TAG = "VsieManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // StateFlow for UI reactive updates
    private val _discoveredMessages = MutableStateFlow<List<VsieMessage>>(emptyList())
    val discoveredMessages: StateFlow<List<VsieMessage>> = _discoveredMessages

    // Persistent Device ID
    val deviceId: String by lazy {
        val prefs = context.getSharedPreferences("VSIE_PREFS", Context.MODE_PRIVATE)
        prefs.getString("DEVICE_ID", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("DEVICE_ID", it).apply()
        }
    }

    // Wi-Fi Aware Manager
    private val wifiAwareManager = context.getSystemService(Context.WIFI_AWARE_SERVICE) as? WifiAwareManager
    private var session: WifiAwareSession? = null

    private val lmtpSegmenter = LmtpSegmenter()

    // Active peers messages
    private val activeMessages = HashMap<String, VsieMessage>()

    fun start() {
        if (wifiAwareManager?.isAvailable != true) {
            Log.e(TAG, "Wi-Fi Aware not available on device")
            return
        }

        wifiAwareManager.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession) {
                this@VsieManager.session = session
                Log.i(TAG, "VSIE Reader/Writer attached successfully")
                startSubscribe()
            }

            override fun onAttachFailed() {
                Log.e(TAG, "VSIE attach failed")
            }
        }, null)
    }

    private fun startSubscribe() {
        val config = SubscribeConfig.Builder()
            .setServiceName("LIFENET_VSIE")
            .setSubscribeType(SubscribeConfig.SUBSCRIBE_TYPE_PASSIVE)
            .build()

        session?.subscribe(config, object : DiscoverySessionCallback() {
            override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                scope.launch {
                    handleIncomingMessage(peerHandle, message)
                }
            }

            override fun onSubscribeFailed() {
                Log.e(TAG, "VSIE subscription failed")
            }
        }, null)
    }

    // Callback for Voice Engine
    var voiceListener: ((net.lifenet.core.voice.VoicePacket) -> Unit)? = null

    private suspend fun handleIncomingMessage(peerHandle: PeerHandle, rawData: ByteArray) {
        if (rawData.isEmpty()) return
        
        val type = rawData[0]
        val payload = rawData.copyOfRange(1, rawData.size)
        
        if (type.toInt() == 0x02) {
            // VOICE PACKET
            try {
                // Deserialize
                val buffer = java.nio.ByteBuffer.wrap(payload)
                val idLen = buffer.get().toInt()
                val idBytes = ByteArray(idLen)
                buffer.get(idBytes)
                val streamId = String(idBytes, Charsets.UTF_8)
                val seq = buffer.getInt()
                val ttl = buffer.getInt()
                val hop = buffer.getInt()
                val voiceData = ByteArray(buffer.remaining())
                buffer.get(voiceData)
                
                val packet = net.lifenet.core.voice.VoicePacket(streamId, seq, ttl, hop, voiceData)
                
                // Dispatch to Engine
                withContext(Dispatchers.Main) {
                     voiceListener?.invoke(packet)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse Voice Packet", e)
            }
        } else {
            // TEXT / LMTP (0x01)
            // Existing Logic handling
             val segments = lmtpSegmenter.reassemble(payload)
             segments.forEach { segPayload ->
                 val msg = VsieMessage(
                     id = UUID.randomUUID().toString(),
                     payload = segPayload,
                     ttl = 5,
                     hops = 0,
                     senderId = peerHandle.toString()
                 )
                 activeMessages[msg.id] = msg
                 _discoveredMessages.emit(activeMessages.values.toList())
                 relayMessage(msg)
             }
        }
    }

    fun sendMessage(payload: ByteArray) {
        scope.launch {
            val segments = lmtpSegmenter.fragment(payload)
            segments.forEach { segment ->
                publishSegment(0x01, segment) // 0x01 = TEXT
            }
        }
    }
    
    // --- Transmit Queue ---
    private fun publishSegment(type: Byte, segment: ByteArray) {
        val finalBytes = ByteArray(1 + segment.size)
        finalBytes[0] = type
        System.arraycopy(segment, 0, finalBytes, 1, segment.size)

        val config = PublishConfig.Builder()
            .setServiceName("LIFENET_VSIE")
            .setTtlSec(if (type.toInt() == 0x02) 20 else 120) // Short TTL for Voice
            .build()
            
        // Priority Logic: Voice skips internal queuing if we had one.
        // Wi-Fi Aware publish is relatively fast, but providing TtlSec hint helps.
        
        session?.publish(config, object : DiscoverySessionCallback() {
            override fun onPublishFailed() {
                Log.e(TAG, "VSIE publish failed (Type $type)")
            }
        }, null)
    }

    private suspend fun relayMessage(msg: VsieMessage) {
        if (msg.ttl <= 0) return
        msg.ttl--
        msg.hops++
        sendMessage(msg.payload)
    }

    fun stop() {
        session?.close()
        scope.cancel()
        Log.i(TAG, "VSIE stopped")
    }
    }

    // --- Voice Extensions ---

    fun broadcastVoice(packet: net.lifenet.core.voice.VoicePacket) {
        val bytes = serializeVoicePacket(packet)
        // Direct Publish for Voice (Low Latency) - Type 0x02
        publishSegment(0x02, bytes)
    }

    fun relayVoice(packet: net.lifenet.core.voice.VoicePacket) {
        if (packet.ttl <= 0) return
        val bytes = serializeVoicePacket(packet)
        publishSegment(0x02, bytes)
    }

    private fun serializeVoicePacket(packet: net.lifenet.core.voice.VoicePacket): ByteArray {
        val idBytes = packet.streamId.toByteArray(Charsets.UTF_8)
        val payload = packet.payload
        val buffer = java.nio.ByteBuffer.allocate(1 + idBytes.size + 4 + 4 + 4 + payload.size)
        buffer.put(idBytes.size.toByte())
        buffer.put(idBytes)
        buffer.putInt(packet.seq)
        buffer.putInt(packet.ttl)
        buffer.putInt(packet.hop)
        buffer.put(payload)
        return buffer.array()
    }
}
