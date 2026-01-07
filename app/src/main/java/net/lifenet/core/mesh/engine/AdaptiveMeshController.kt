package net.lifenet.core.mesh.engine

import android.util.Log
import net.lifenet.core.mode.LifenetMode

/**
 * AdaptiveMeshController: Self-tuning broadcast and energy controller.
 */
class AdaptiveMeshController {

    private var currentIntervalMs: Long = 5000
    private var txPower: Int = 1 // 0: Min, 1: Med, 2: Max

    private var currentEpochMs: Long = 2000
    private val MIN_EPOCH = 500L
    private val MAX_EPOCH = 5000L
    private val CONGESTION_THRESHOLD = 70

    fun tune(score: Int, mode: LifenetMode = LifenetMode.DAILY) {

        // Mode-Aware Interval Scaling
        currentIntervalMs = if (mode == LifenetMode.DISASTER) {
            when {
                score < 15 -> 3000L
                score < 40 -> 7000L
                score < 70 -> 15000L
                else -> 30000L
            }
        } else {
            when {
                score < 30 -> 10000L
                score < 60 -> 20000L
                else -> 60000L
            }
        }

        // Mode-Aware QoS / Epoch Tuning
        currentEpochMs = if (mode == LifenetMode.DISASTER) {
            when {
                score > CONGESTION_THRESHOLD ->
                    (currentEpochMs - 800).coerceAtLeast(MIN_EPOCH)
                score < 30 ->
                    (currentEpochMs + 1000).coerceAtMost(MAX_EPOCH)
                else -> currentEpochMs
            }
        } else {
            (currentEpochMs + 500).coerceAtMost(MAX_EPOCH)
        }

        // TX Power Scaling
        txPower = when {
            score < 10 -> 2
            score < 50 -> 1
            else -> 0
        }

        Log.i(
            "LIFENET",
            "AdaptiveMeshController | Mode=$mode Epoch=${currentEpochMs}ms Interval=${currentIntervalMs}ms TX=$txPower"
        )
    }

    fun getCurrentEpochMs(): Long = currentEpochMs
    fun getBroadcastInterval(): Long = currentIntervalMs
    fun getTxPower(): Int = txPower
}
