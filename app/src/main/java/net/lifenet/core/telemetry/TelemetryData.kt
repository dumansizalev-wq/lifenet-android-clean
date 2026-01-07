package net.lifenet.core.telemetry

enum class HealthStatus {
    EXCELLENT, GOOD, WARNING, CRITICAL, OFFLINE
}

data class TelemetryData(
    val timestamp: Long = System.currentTimeMillis(),
    val packetCollisionRate: Float = 0f,
    val averageLatency: Float = 0f,
    val meshCongestion: Float = 0f,
    val activePeers: Int = 0,
    val messagesPerSecond: Float = 0f,
    val compressionSavings: Float = 0f // % of data saved
)

data class MeshHealth(
    val status: HealthStatus,
    val congestionLevel: Float,
    val peerCount: Int,
    val signalStrength: Float
)
