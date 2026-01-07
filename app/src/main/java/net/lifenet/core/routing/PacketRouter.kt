package net.lifenet.core.routing

import android.util.Log
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * PacketRouter: The deterministic traffic controller for the mesh node.
 * Handles TTL enforcement, hop counts, and replay protection.
 */
class PacketRouter(private val nodeId: String) {

    private val seenPackets = ConcurrentHashMap.newKeySet<Long>()
    private val MAX_HOPS = 15
    private val CACHE_SIZE_LIMIT = 5000

    fun activePeerCount(): Int = seenPackets.size

    /**
     * DataPacket Structure (Binary):
     * [0..7]   PacketID (Long)
     * [8]      TTL (Byte)
     * [9]      HopCount (Byte)
     * [10..41] SourceID (32-byte Hash)
     * [42..73] TargetID (32-byte Hash)
     * [74..]   Payload
     */
    fun onReceivePacket(rawPacket: ByteArray): RoutingDecision {
        if (rawPacket.size < 74) return RoutingDecision.DROP_INVALID

        val buffer = ByteBuffer.wrap(rawPacket)
        val packetId = buffer.long
        
        // Replay Protection
        if (seenPackets.contains(packetId)) {
            return RoutingDecision.DROP_DUPLICATE
        }
        
        // Maintain Cache Size
        if (seenPackets.size > CACHE_SIZE_LIMIT) {
            seenPackets.clear() // Simple flush
        }
        seenPackets.add(packetId)

        val ttl = buffer.get().toInt()
        val hopCount = buffer.get().toInt()

        // TTL & Hop Checking
        if (ttl <= 0 || hopCount >= MAX_HOPS) {
            return RoutingDecision.DROP_EXPIRED
        }

        val sourceBytes = ByteArray(32)
        buffer.get(sourceBytes)
        val targetBytes = ByteArray(32)
        buffer.get(targetBytes)
        
        val targetId = String(targetBytes).trim()

        return when {
            targetId == nodeId -> RoutingDecision.CONSUME
            else -> RoutingDecision.FORWARD
        }
    }

    enum class RoutingDecision {
        CONSUME,
        FORWARD,
        DROP_DUPLICATE,
        DROP_EXPIRED,
        DROP_INVALID
    }

    fun prepareForForwarding(rawPacket: ByteArray): ByteArray {
        val buffer = ByteBuffer.wrap(rawPacket)
        buffer.position(8)
        
        val currentTtl = buffer.get()
        val currentHops = buffer.get()
        
        buffer.position(8)
        buffer.put((currentTtl - 1).toByte())
        buffer.put((currentHops + 1).toByte())
        
        return rawPacket
    }
}
