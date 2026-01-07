package net.lifenet.core.messaging.qos

import net.lifenet.core.mode.LifenetMode
import net.lifenet.core.mode.ModeManager
import net.lifenet.core.data.MessageType
import net.lifenet.core.lifecycle.ResourceMonitor
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

/**
 * QoSController için unit testler
 */
class QoSControllerTest {
    
    private lateinit var modeManager: ModeManager
    private lateinit var resourceMonitor: ResourceMonitor
    private lateinit var qosController: QoSController
    
    @Before
    fun setup() {
        modeManager = mock(ModeManager::class.java)
        resourceMonitor = mock(ResourceMonitor::class.java)
        qosController = QoSController(modeManager, resourceMonitor)
        
        // Default mock behavior
        `when`(resourceMonitor.getBatteryLevel()).thenReturn(80)
    }
    
    @Test
    fun testCriticalAlwaysHighPriority() {
        val metrics = RuntimeMetrics(
            currentMode = LifenetMode.DAILY,
            peerCount = 5,
            queueDepth = 10,
            batteryLevel = 80,
            networkStabilityScore = 0.8f
        )
        
        val priority = qosController.calculateQoSPriority(MessageType.SOS, metrics)
        
        assertTrue("CRITICAL messages should have priority >= 100", priority >= 100)
    }
    
    @Test
    fun testDailyModeStaticPriority() {
        val metrics = RuntimeMetrics(
            currentMode = LifenetMode.DAILY,
            peerCount = 50,  // Yüksek peer count
            queueDepth = 200,  // Yüksek queue
            batteryLevel = 10,  // Düşük batarya
            networkStabilityScore = 0.2f  // Kötü ağ
        )
        
        // DAILY modda NORMAL mesajlar statik önceliğe sahip olmalı
        val priority = qosController.calculateQoSPriority(MessageType.TEXT, metrics)
        assertEquals(50, priority)  // NORMAL default priority
    }
    
    @Test
    fun testDisasterModeDynamicPriority() {
        val metrics = RuntimeMetrics(
            currentMode = LifenetMode.DISASTER,
            peerCount = 25,  // Yüksek peer count (>20)
            queueDepth = 50,
            batteryLevel = 80,
            networkStabilityScore = 0.8f
        )
        
        val priority = qosController.calculateQoSPriority(MessageType.TEXT, metrics)
        
        // DISASTER modda yüksek peer count ile NORMAL priority azalmalı
        assertTrue("Priority should be reduced in DISASTER mode with high peer count", 
            priority < 50)
    }
    
    @Test
    fun testBatteryLowBulkStop() {
        val metrics = RuntimeMetrics(
            currentMode = LifenetMode.DISASTER,
            peerCount = 5,
            queueDepth = 10,
            batteryLevel = 15,  // Kritik batarya (<20%)
            networkStabilityScore = 0.8f
        )
        
        val priority = qosController.calculateQoSPriority(MessageType.LOG, metrics)
        
        assertEquals("BULK should be stopped when battery is critical", 0, priority)
    }
    
    @Test
    fun testHighQueueDepthBulkStop() {
        val metrics = RuntimeMetrics(
            currentMode = LifenetMode.DISASTER,
            peerCount = 5,
            queueDepth = 150,  // Yüksek queue (>100)
            batteryLevel = 80,
            networkStabilityScore = 0.8f
        )
        
        val priority = qosController.calculateQoSPriority(MessageType.SYNC, metrics)
        
        assertEquals("BULK should be stopped when queue is high", 0, priority)
    }
    
    @Test
    fun testShouldSendMessageCritical() {
        val metrics = RuntimeMetrics(
            currentMode = LifenetMode.DISASTER,
            peerCount = 50,
            queueDepth = 200,
            batteryLevel = 5,
            networkStabilityScore = 0.1f
        )
        
        // CRITICAL mesajlar her koşulda gönderilmeli
        assertTrue(qosController.shouldSendMessage(MessageType.SOS, metrics))
    }
    
    @Test
    fun testShouldNotSendBulkWhenBatteryLow() {
        val metrics = RuntimeMetrics(
            currentMode = LifenetMode.DISASTER,
            peerCount = 5,
            queueDepth = 10,
            batteryLevel = 15,
            networkStabilityScore = 0.8f
        )
        
        assertFalse("BULK should not be sent when battery is low",
            qosController.shouldSendMessage(MessageType.LOG, metrics))
    }
    
    @Test
    fun testCalculateDelayCriticalZero() {
        val metrics = RuntimeMetrics(
            currentMode = LifenetMode.DISASTER,
            peerCount = 5,
            queueDepth = 10,
            batteryLevel = 80,
            networkStabilityScore = 0.8f
        )
        
        val delay = qosController.calculateDelay(MessageType.PTT, metrics)
        
        assertEquals("CRITICAL messages should have zero delay", 0L, delay)
    }
    
    @Test
    fun testCalculateDelayBulkHighInDisaster() {
        val metrics = RuntimeMetrics(
            currentMode = LifenetMode.DISASTER,
            peerCount = 25,
            queueDepth = 50,
            batteryLevel = 80,
            networkStabilityScore = 0.8f
        )
        
        val delay = qosController.calculateDelay(MessageType.FILE, metrics)
        
        assertTrue("BULK messages should have significant delay in DISASTER mode",
            delay >= 2000L)
    }
    
    @Test
    fun testNetworkUnstableCriticalBoost() {
        val metrics = RuntimeMetrics(
            currentMode = LifenetMode.DISASTER,
            peerCount = 5,
            queueDepth = 10,
            batteryLevel = 80,
            networkStabilityScore = 0.3f  // Kararsız ağ
        )
        
        val priority = qosController.calculateQoSPriority(MessageType.SOS, metrics)
        
        assertTrue("CRITICAL priority should be boosted when network is unstable",
            priority > 100)
    }
    
    @Test
    fun testRuntimeMetricsValidation() {
        // Geçersiz batarya seviyesi
        try {
            RuntimeMetrics(
                currentMode = LifenetMode.DAILY,
                peerCount = 5,
                queueDepth = 10,
                batteryLevel = 150,  // Geçersiz
                networkStabilityScore = 0.8f
            )
            fail("Should throw exception for invalid battery level")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
    
    @Test
    fun testPeerCountScaling() {
        val lowPeerMetrics = RuntimeMetrics(
            currentMode = LifenetMode.DISASTER,
            peerCount = 5,
            queueDepth = 10,
            batteryLevel = 80,
            networkStabilityScore = 0.8f
        )
        
        val highPeerMetrics = RuntimeMetrics(
            currentMode = LifenetMode.DISASTER,
            peerCount = 30,
            queueDepth = 10,
            batteryLevel = 80,
            networkStabilityScore = 0.8f
        )
        
        val lowPeerPriority = qosController.calculateQoSPriority(MessageType.TEXT, lowPeerMetrics)
        val highPeerPriority = qosController.calculateQoSPriority(MessageType.TEXT, highPeerMetrics)
        
        assertTrue("Priority should decrease with higher peer count",
            highPeerPriority < lowPeerPriority)
    }
}
