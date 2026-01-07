package net.lifenet.core.lifecycle

import net.lifenet.core.mesh.engine.MeshStateAnalyzer
import net.lifenet.core.messaging.MessageStore
import net.lifenet.core.routing.PacketRouter
import net.lifenet.core.radio.RadioStatsProvider
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MeshTelemetryIngestor(
    private val analyzer: MeshStateAnalyzer,
    private val messageStore: MessageStore,
    private val packetRouter: PacketRouter,
    private val radioStats: RadioStatsProvider
) {

    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    fun start() {
        scheduler.scheduleAtFixedRate(
            { collectAndRecord() },
            0,
            10,
            TimeUnit.SECONDS
        )
    }

    fun stop() {
        scheduler.shutdownNow()
    }

    private fun collectAndRecord() {
        val peerCount = packetRouter.activePeerCount()

        val sent = radioStats.sentPackets()
        val acked = radioStats.ackedPackets()
        val collisionRate =
            if (sent == 0) 0f else ((sent - acked).toFloat() / sent.toFloat()).coerceIn(0f, 1f)

        val queuePressure =
            (messageStore.currentSize().toFloat() / messageStore.maxSize().toFloat())
                .coerceIn(0f, 1f)

        // RSSI Collection (Strictly RAW negative dBm)
        val noiseFloor = radioStats.recentRssiSamples()
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toInt()
            ?: -90

        analyzer.recordTelemetry(
            MeshStateAnalyzer.MeshTelemetry(
                peerCount = peerCount,
                packetCollisionRate = collisionRate,
                noiseFloor = noiseFloor,
                queuePressure = queuePressure
            )
        )
    }
}
