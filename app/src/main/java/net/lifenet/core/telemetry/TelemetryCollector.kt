package net.lifenet.core.telemetry

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.lifenet.core.mesh.GhostRadioService
import kotlin.random.Random

class TelemetryCollector(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _telemetryFlow = MutableStateFlow(TelemetryData())
    val telemetryFlow: StateFlow<TelemetryData> = _telemetryFlow
    
    private var ghostService: GhostRadioService? = null
    private var isCollecting = false
    
    fun attachService(service: GhostRadioService) {
        ghostService = service
    }
    
    fun startCollection() {
        if (isCollecting) return
        isCollecting = true
        
        scope.launch {
            while (isCollecting) {
                collectTelemetry()
                delay(1500) // Update every 1.5 seconds
            }
        }
    }
    
    fun stopCollection() {
        isCollecting = false
    }
    
    private fun collectTelemetry() {
        val service = ghostService
        
        val telemetry = if (service != null) {
            TelemetryData(
                timestamp = System.currentTimeMillis(),
                packetCollisionRate = service.getPacketCollisionRate(),
                averageLatency = service.getAverageLatency(),
                meshCongestion = service.getMeshCongestion(),
                activePeers = service.getConnectedPeersCount(),
                messagesPerSecond = calculateMessageRate(),
                compressionSavings = 35.5f // TODO: Real metric from MessagingService
            )
        } else {
            // Fallback with simulated data for testing
            TelemetryData(
                timestamp = System.currentTimeMillis(),
                packetCollisionRate = Random.nextFloat() * 10f,
                averageLatency = 50f + Random.nextFloat() * 100f,
                meshCongestion = Random.nextFloat() * 100f,
                activePeers = Random.nextInt(0, 10),
                messagesPerSecond = Random.nextFloat() * 5f,
                compressionSavings = 30f + Random.nextFloat() * 20f
            )
        }
        
        _telemetryFlow.value = telemetry
    }
    
    private fun calculateMessageRate(): Float {
        // TODO: Implement actual message rate calculation
        return Random.nextFloat() * 5f
    }
    
    fun getMeshHealth(): MeshHealth {
        val telemetry = _telemetryFlow.value
        
        val status = when {
            telemetry.meshCongestion > 80f || telemetry.activePeers == 0 -> HealthStatus.CRITICAL
            telemetry.meshCongestion > 60f || telemetry.packetCollisionRate > 15f -> HealthStatus.WARNING
            telemetry.meshCongestion > 30f -> HealthStatus.GOOD
            else -> HealthStatus.EXCELLENT
        }
        
        return MeshHealth(
            status = status,
            congestionLevel = telemetry.meshCongestion,
            peerCount = telemetry.activePeers,
            signalStrength = 100f - telemetry.meshCongestion
        )
    }
}
