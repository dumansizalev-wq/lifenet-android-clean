package net.lifenet.core.mesh.engine

import kotlin.math.min
import java.util.ArrayDeque

class MeshStateAnalyzer {

    data class MeshTelemetry(
        val peerCount: Int,
        val packetCollisionRate: Float,
        val noiseFloor: Int, // Raw negative dBm (e.g., -90)
        val queuePressure: Float
    )

    private val windowSize = 5
    private val telemetryWindow: ArrayDeque<MeshTelemetry> = ArrayDeque(windowSize)

    fun recordTelemetry(telemetry: MeshTelemetry) {
        if (telemetryWindow.size == windowSize) {
            telemetryWindow.removeFirst()
        }
        telemetryWindow.addLast(telemetry)
    }

    /**
     * Produces a deterministic congestion score (0-100).
     * Higher score = Higher congestion/instability.
     */
    fun currentCongestionScore(): Int {
        if (telemetryWindow.isEmpty()) return 0
        val avg = movingAverage()

        // Signal Badness Factor (30% weight):
        // -100 dBm (Quiet/Good) -> 0.0
        // -40 dBm (Noisy/Interference) -> 1.0
        val signalBadness =
            ((avg.noiseFloor + 100).toFloat() / 60f).coerceIn(0f, 1f)

        val score =
            (avg.packetCollisionRate * 35f) +
                    (min(avg.peerCount / 40f, 1f) * 20f) +
                    (avg.queuePressure * 15f) +
                    (signalBadness * 30f)

        return score.toInt().coerceIn(0, 100)
    }

    /**
     * Map congestion score to transmission delay in milliseconds.
     */
    fun delayForScore(score: Int): Long {
        return when {
            score < 20 -> 0L
            score < 40 -> 500L
            score < 60 -> 1500L
            score < 80 -> 3000L
            else -> 6000L
        }
    }

    private fun movingAverage(): MeshTelemetry {
        var peerSum = 0
        var collisionSum = 0f
        var noiseSum = 0
        var queueSum = 0f

        telemetryWindow.forEach {
            peerSum += it.peerCount
            collisionSum += it.packetCollisionRate
            noiseSum += it.noiseFloor
            queueSum += it.queuePressure
        }

        val count = telemetryWindow.size.coerceAtLeast(1)

        return MeshTelemetry(
            peerCount = peerSum / count,
            packetCollisionRate = collisionSum / count,
            noiseFloor = noiseSum / count,
            queuePressure = queueSum / count
        )
    }
}
