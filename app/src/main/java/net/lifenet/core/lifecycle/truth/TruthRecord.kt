package net.lifenet.core.lifecycle.truth

/**
 * TruthRecord: The minimal data set for node resurrection.
 */
data class TruthRecord(
    val nodeId: String,
    val lastSequence: Long,
    val lastKnownStateHash: String,
    val atomicSeal: String,
    val timestamp: Long
)
