package net.lifenet.core.messaging.forward

import net.lifenet.core.routing.RouteScoreEngine

/**
 * ForwardingPolicy: Rules for mesh message distribution.
 */
object ForwardingPolicy {

    const val MIN_SCORE_TO_FORWARD = 0.4
    const val MAX_HOPS_ALLOWED = 10

    fun shouldForward(score: Double, envelope: net.lifenet.core.messaging.MessageEnvelope): Boolean {
        return score >= MIN_SCORE_TO_FORWARD && envelope.hopCount < MAX_HOPS_ALLOWED
    }
}
