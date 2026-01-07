package net.lifenet.core.transport.aware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer

/**
 * Manages Wi-Fi Aware (NAN) "Silent Signal" transport.
 * Implements the Short-Ephemeric Protocol defined in WIFI_AWARE_PROTOCOL_SPEC.md.
 */
class WifiAwareTransportManager(private val context: Context) {

    private val TAG = "WifiAwareTransport"
    private val SERVICE_ID = "Lifenet"

    private val wifiAwareManager: WifiAwareManager? =
        context.getSystemService(Context.WIFI_AWARE_SERVICE) as? WifiAwareManager

    private var currentSession: WifiAwareSession? = null
    private var publishSession: PublishDiscoverySession? = null
    private var subscribeSession: SubscribeDiscoverySession? = null
    
    private val _isAwareAvailable = MutableStateFlow(false)
    val isAwareAvailable: StateFlow<Boolean> = _isAwareAvailable

    // Old Interface Removed

    private var listener: AwarenessListener? = null

    fun setListener(listener: AwarenessListener) {
        this.listener = listener
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED) {
                val available = wifiAwareManager?.isAvailable ?: false
                _isAwareAvailable.value = available
                Log.d(TAG, "Wi-Fi Aware State Changed: Available=$available")
                if (available) attach()
            }
        }
    }

    init {
        val filter = IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED)
        context.registerReceiver(broadcastReceiver, filter)
        
        // Auto-Start Logic for Emergency Release
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
             if (wifiAwareManager?.isAvailable == true) {
                 _isAwareAvailable.value = true
                 Log.i(TAG, "Wi-Fi Aware Available on Init - Auto-Starting...")
                 attach()
             } else {
                 Log.w(TAG, "Wi-Fi Aware Manager not available yet.")
             }
        } else {
            Log.e(TAG, "Device does not support Wi-Fi Aware!")
        }
    }

    private fun attach() {
        if (currentSession != null) return
        
        wifiAwareManager?.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession) {
                Log.i(TAG, "Wi-Fi Aware Session Attached")
                currentSession = session
                startAdvertising() // Start Publishing immediately (Passive Signal)
                startListening()   // Start Subscribing immediately
            }

            override fun onAttachFailed() {
                Log.e(TAG, "Wi-Fi Aware Attach Failed")
            }
        }, null)
    }

    private fun getDeviceId(): ByteArray {
        val prefs = context.getSharedPreferences("lifenet_identity", Context.MODE_PRIVATE)
        val stored = prefs.getString("device_id_hex", null)
        return if (stored != null) {
            hexStringToByteArray(stored)
        } else {
            val newId = ByteArray(12)
            java.security.SecureRandom().nextBytes(newId)
            prefs.edit().putString("device_id_hex", byteArrayToHexString(newId)).apply()
            Log.i(TAG, "Generated New Device ID: ${byteArrayToHexString(newId)}")
            newId
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun byteArrayToHexString(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    /**
     * Publishes the "Heartbeat" signal.
     */
    fun startAdvertising() {
        val session = currentSession ?: return
        
        // Protocol Payload Construction
        // MAGIC(1) + VER(1) + TYPE(1) + TS(4) + DEV_ID(12) + DATA(1) = ~20 bytes
        val buffer = ByteBuffer.allocate(20)
        buffer.put(0x4C.toByte()) // MAGIC (L for Lifenet)
        buffer.put(0x01.toByte()) // VERSION
        buffer.put(0x01.toByte()) // TYPE: Heartbeat
        buffer.putInt((System.currentTimeMillis() / 1000).toInt()) // TS
        // Real Device ID
        buffer.put(getDeviceId()) 
        buffer.put(0x64.toByte()) // Battery: 100%
        
        val config = PublishConfig.Builder()
            .setServiceName(SERVICE_ID)
            .setServiceSpecificInfo(buffer.array())
            .setPublishType(PublishConfig.PUBLISH_TYPE_UNSOLICITED) // Broadcast
            .build()
            
        session.publish(config, object : DiscoverySessionCallback() {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                Log.i(TAG, "Publish Started: Silent Signal Active")
                publishSession = session
            }
            
            override fun onSessionConfigFailed() {
                Log.e(TAG, "Publish Failed")
            }
        }, null)
    }

    /**
     * Listens for other LIFENET signals.
     */
    fun startListening() {
        val session = currentSession ?: return
        
        val config = SubscribeConfig.Builder()
            .setServiceName(SERVICE_ID)
            .setSubscribeType(SubscribeConfig.SUBSCRIBE_TYPE_PASSIVE) // Listen only
            .build()
            
        session.subscribe(config, object : DiscoverySessionCallback() {
            override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                Log.i(TAG, "Subscribe Started: Listening for Silent Signals")
                subscribeSession = session
            }

            override fun onServiceDiscovered(
                peerHandle: PeerHandle,
                serviceSpecificInfo: ByteArray,
                matchFilter: MutableList<ByteArray>
            ) {
                Log.i(TAG, "Silent Signal Received! Len=${serviceSpecificInfo.size}")
                listener?.onSignalDetected(peerHandle, serviceSpecificInfo)
                parseSignal(serviceSpecificInfo)
            }
        }, null)
    }
    
    // --- V2 PROTOCOL IMPLEMENTATION ---

    fun publishMessage(message: net.lifenet.core.data.LifenetMessage) {
        val session = publishSession ?: return
        
        val payloadBytes = message.content.toByteArray(Charsets.UTF_8)
        val targetIdBytes = if (message.targetId == "BROADCAST") ByteArray(12) else hexStringToByteArray(message.targetId)
        
        // Padded/Truncated Target ID (12 bytes)
        val finalTargetIdBytes = ByteArray(12)
        System.arraycopy(targetIdBytes, 0, finalTargetIdBytes, 0, minOf(targetIdBytes.size, 12))
        
        // V2 Header: 
        // MAGIC(1)=0x4C, VER(1)=0x02, TYPE(1)=0x04 (MSG), TS(4), SENDER(12), MSG_ID(8 Long), TTL(1), HOPS(1) = 29 bytes
        
        val buffer = ByteBuffer.allocate(29 + payloadBytes.size + 4) // +4 Checksum
        buffer.put(0x4C.toByte()) 
        buffer.put(0x02.toByte()) // V2
        buffer.put(0x04.toByte()) // TYPE MSG
        buffer.putInt((System.currentTimeMillis() / 1000).toInt())
        buffer.put(getDeviceId()) // Sender ID (12)
        
        // Msg ID (Hash of UUID string as Long for compact transport)
        buffer.putLong(message.id.hashCode().toLong())
        buffer.put(message.ttl.toByte())
        buffer.put(message.hops.toByte())
        
        // Payload: Target ID (12 in Payload section per spec? Or separate?)
        // Spec said: Payload = TargetDeviceID(12) + Content
        buffer.put(finalTargetIdBytes)
        buffer.put(payloadBytes)
        
        // Checksum (Simple Hash)
        buffer.putInt(payloadBytes.hashCode())
        
        val packet = buffer.array()
        Log.d(TAG, "Publishing V2 Message ${message.id} to ${message.targetId} (TTL=${message.ttl})")
        
        val updateConfig = PublishConfig.Builder()
            .setServiceName(SERVICE_ID)
            .setServiceSpecificInfo(packet)
            .setPublishType(PublishConfig.PUBLISH_TYPE_UNSOLICITED)
            .build()
            
        session.updatePublish(updateConfig)
    }

    private val _discoveredPeers = MutableStateFlow<List<String>>(emptyList())
    val discoveredPeers: StateFlow<List<String>> = _discoveredPeers
    
    // Map to track last seen time for cleanup (not implemented full cleanup logic for emergency release)
    private val peerLastSeenMap = mutableMapOf<String, Long>()

    private fun parseSignal(data: ByteArray) {
        if (data.size < 20) return // Min size for any V1/V2 packet
        val buffer = ByteBuffer.wrap(data)
        
        val magic = buffer.get()
        if (magic != 0x4C.toByte()) return
        
        val ver = buffer.get()
        val type = buffer.get()
        
        // HEARTBEAT (Type 0x01)
        if (type == 0x01.toByte()) {
             val ts = buffer.getInt()
             val senderIdBytes = ByteArray(12)
             buffer.get(senderIdBytes)
             val senderId = byteArrayToHexString(senderIdBytes)
             
             peerLastSeenMap[senderId] = System.currentTimeMillis()
             _discoveredPeers.value = peerLastSeenMap.keys.toList()
             Log.d(TAG, "Discovered Peer via Heartbeat: $senderId")
             return
        }
        
        // MESSAGE (Type 0x04) handling continues...
        if (type == 0x04.toByte()) {
            val ts = buffer.getInt() // Read TS to stay in sync
            val senderIdBytes = ByteArray(12)
            buffer.get(senderIdBytes)
            val senderId = byteArrayToHexString(senderIdBytes)
        
            val msgIdHash = buffer.getLong()
            val ttl = buffer.get()
            val hops = buffer.get()
            
            // Payload = Target(12) + Content
            val targetIdBytes = ByteArray(12)
            buffer.get(targetIdBytes)
            val targetId = byteArrayToHexString(targetIdBytes)
            
            val contentLen = buffer.remaining() - 4 // Checksum
            if (contentLen > 0) {
                val contentBytes = ByteArray(contentLen)
                buffer.get(contentBytes)
                val content = String(contentBytes, Charsets.UTF_8)
                
                // Notify Listener with extended info
                listener?.onMessageReceived(senderId, targetId, msgIdHash, ttl, hops, content)
            }
        }
    }
    
    // Updated Interface
    interface AwarenessListener {
        fun onSignalDetected(peerHandle: PeerHandle, data: ByteArray) {} // Legacy
        fun onMessageReceived(senderId: String, targetId: String, msgIdHash: Long, ttl: Byte, hops: Byte, content: String)
    }

    fun publishToDevice(deviceId: String, payload: ByteArray) {
        // Deprecated in favor of publishMessage
    }

    fun cleanup() {
        context.unregisterReceiver(broadcastReceiver)
        currentSession?.close()
        currentSession = null
        listener = null
    }
}
