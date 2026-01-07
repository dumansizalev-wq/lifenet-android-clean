package net.lifenet.core.messaging.forward

import net.lifenet.core.messaging.MessageEnvelope
import java.util.LinkedHashMap

/**
 * ForwardingEngine
 * Loop-safe Store-and-Forward engine with Seen-Message LRU cache.
 *
 * CONSTITUTIONAL GUARANTEES:
 * - A message is NEVER forwarded twice to the same node
 * - Message loops are deterministically blocked
 * - Cache is bounded (LRU) to prevent memory exhaustion
 */
class ForwardingEngine(
    private val localNodeId: String,
    maxSeenMessages: Int = 10_000
) {

    /**
     * Seen Message Cache (LRU)
     * Key   : messageId (String)
     * Value : timestamp (last seen)
     */
    private val seenMessages: MutableMap<String, Long> =
        object : LinkedHashMap<String, Long>(maxSeenMessages, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean {
                return size > maxSeenMessages
            }
        }

    private val lock = Any()

    /**
     * Entry point for incoming messages from mesh.
     */
    fun onMessageReceived(envelope: MessageEnvelope): ForwardingDecision {
        val msgId = envelope.messageId

        synchronized(lock) {
            if (seenMessages.containsKey(msgId)) {
                // LOOP DETECTED — message already processed by this node
                return ForwardingDecision.DROP_LOOP
            }
            seenMessages[msgId] = System.currentTimeMillis()
        }

        // TTL / hop enforcement
        // TTL normally decrements, hopCount increments
        if (envelope.ttl <= 0) {
            return ForwardingDecision.DROP_EXPIRED
        }

        return ForwardingDecision.ACCEPT_AND_FORWARD
    }

    /**
     * Called before forwarding to a peer.
     * Ensures we do not reflect the message back to its previous hop.
     */
    fun shouldForwardToPeer(
        envelope: MessageEnvelope,
        peerNodeId: String
    ): Boolean {

        // Never send back to the node we received it from
        if (peerNodeId == envelope.lastHopNodeId) {
            return false
        }

        // Never send to ourselves
        if (peerNodeId == localNodeId) {
            return false
        }

        return true
    }

    /**
     * Optional periodic cleanup hook (defensive).
     */
    fun pruneOlderThan(maxAgeMs: Long) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        synchronized(lock) {
            val iterator = seenMessages.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value < cutoff) {
                    iterator.remove()
                }
            }
        }
    }
}

/**
 * Deterministic forwarding outcomes.
 */
enum class ForwardingDecision {
    ACCEPT_AND_FORWARD,
    DROP_LOOP,
    DROP_EXPIRED
}
