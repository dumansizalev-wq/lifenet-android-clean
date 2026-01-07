package net.lifenet.core.messaging.routing

import net.lifenet.core.messaging.MessageEnvelope
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

class RoutingEngine(
    private val ownNodeId: String,
    private val onMessageToMe: (MessageEnvelope) -> Unit,
    private val onRelayRequired: (MessageEnvelope) -> Unit
) {

    // Cache of seen message IDs to prevent infinite loops (Relay loops)
    // Key: MessageId, Value: Timestamp of arrival
    private val seenMessages = ConcurrentHashMap<String, Long>()
    private val HISTORY_EXPIRY_MS = 300_000 // 5 minutes

    /**
     * Entry point for messages arriving from any transport (BLE, Wi-Fi, etc.)
     */
    fun processIncomingEnvelope(envelope: MessageEnvelope) {
        // 1. Cleanup old history occasionally
        cleanupHistory()

        // 2. Already seen this message? Ignore to prevent loops.
        if (seenMessages.containsKey(envelope.messageId)) {
            return
        }
        
        // Mark as seen
        seenMessages[envelope.messageId] = System.currentTimeMillis()

        Log.d("RoutingEngine", "Processing packet: ID=${envelope.messageId} from=${envelope.sourceId} target=${envelope.targetId} hops=${envelope.hopCount} ttl=${envelope.ttl}")

        // 3. Is it for me?
        if (envelope.targetId == ownNodeId || envelope.targetId == "BROADCAST") {
            onMessageToMe(envelope)
        }

        // 4. Should I relay it?
        // Only relay if it's NOT just for me (Broadcast counts as needing relay too)
        // AND if TTL allows it.
        if ((envelope.targetId != ownNodeId) && envelope.ttl > 0) {
            val updatedEnvelope = envelope.copy(
                lastHopNodeId = ownNodeId,
                ttl = envelope.ttl - 1,
                hopCount = envelope.hopCount + 1
            )
            Log.i("RoutingEngine", "Relaying packet ID=${envelope.messageId} (New TTL: ${updatedEnvelope.ttl})")
            onRelayRequired(updatedEnvelope)
        }
    }

    private fun cleanupHistory() {
        val now = System.currentTimeMillis()
        val it = seenMessages.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (now - entry.value > HISTORY_EXPIRY_MS) {
                it.remove()
            }
        }
    }
}
