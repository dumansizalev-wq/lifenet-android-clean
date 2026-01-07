import net.lifenet.core.data.MessageType

import android.os.Handler
import android.os.HandlerThread
import net.lifenet.core.mesh.engine.MeshStateAnalyzer
import net.lifenet.core.messaging.MessageEnvelope
import net.lifenet.core.messaging.qos.QoSController
import net.lifenet.core.mesh.engine.MetricCollector
import java.util.concurrent.atomic.AtomicBoolean

/**
 * DispatchScheduler: QoS-aware message dispatch scheduler
 * Integrates with QoSController for priority-based dispatching
 */
class DispatchScheduler(
    private val analyzer: MeshStateAnalyzer,
    private val qosController: QoSController? = null  // Optional QoS integration
) {

    private val isDispatching = AtomicBoolean(false)
    private val handlerThread = HandlerThread("LifenetDispatch").apply { start() }
    private val handler = Handler(handlerThread.looper)

    /**
     * QoS-aware dispatch request
     */
    fun requestDispatch(
        message: MessageEnvelope,
        action: () -> Unit
    ) {
        if (!isDispatching.compareAndSet(false, true)) return

        val score = analyzer.currentCongestionScore()
        
        // QoS integration
        if (qosController != null) {
            val metrics = collectRuntimeMetrics(score)
            
            // Check if message should be sent
            if (!qosController.shouldSendMessage(message.messageType, metrics)) {
                isDispatching.set(false)
                MetricCollector.incrementQosBulkDropped()
                return
            }
            
            // Calculate QoS-aware delay
            val qosDelay = qosController.calculateDelay(message.messageType, metrics)
            val congestionDelay = analyzer.delayForScore(score)
            val totalDelay = maxOf(qosDelay, congestionDelay)
            
            // Track metrics
            trackQoSMetrics(message.messageType)
            MetricCollector.updateQosAverageDelay(totalDelay)
            
            handler.postDelayed({
                try {
                    action()
                } finally {
                    isDispatching.set(false)
                }
            }, totalDelay)
        } else {
            // Fallback: Original behavior without QoS
            if (score >= 95) {
                isDispatching.set(false)
                return
            }

            val delayMs = analyzer.delayForScore(score)

            handler.postDelayed({
                try {
                    action()
                } finally {
                    isDispatching.set(false)
                }
            }, delayMs)
        }
    }
    
    /**
     * Legacy dispatch without QoS (backward compatibility)
     */
    fun requestDispatch(action: () -> Unit) {
        if (!isDispatching.compareAndSet(false, true)) return

        val score = analyzer.currentCongestionScore()
        
        if (score >= 95) {
            isDispatching.set(false)
            return
        }

        val delayMs = analyzer.delayForScore(score)

        handler.postDelayed({
            try {
                action()
            } finally {
                isDispatching.set(false)
            }
        }, delayMs)
    }
    
    private fun collectRuntimeMetrics(congestionScore: Int): net.lifenet.core.messaging.qos.RuntimeMetrics {
        // Simplified metrics collection
        return net.lifenet.core.messaging.qos.RuntimeMetrics(
            currentMode = net.lifenet.core.mode.LifenetMode.DAILY,  // Will be updated by QoSController
            peerCount = congestionScore / 5,  // Approximate peer count from congestion
            queueDepth = congestionScore,
            batteryLevel = 80,  // Will be updated by QoSController
            networkStabilityScore = (100 - congestionScore) / 100f
        )
    }
    
    private fun trackQoSMetrics(messageType: MessageType) {
        when (messageType.defaultQoS) {
            net.lifenet.core.messaging.qos.QoSLevel.CRITICAL -> MetricCollector.incrementQosCriticalSent()
            net.lifenet.core.messaging.qos.QoSLevel.NORMAL -> MetricCollector.incrementQosNormalSent()
            net.lifenet.core.messaging.qos.QoSLevel.BULK -> MetricCollector.incrementQosBulkSent()
        }
    }

    fun shutdown() {
        handlerThread.quitSafely()
    }
}
