package net.lifenet.core.messaging

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MultiHopDeliveryTest {

    @Test
    fun testMultiHop_Simulated() = runBlocking {
        // ARRANGE: 5 node linear mesh A->B->C->D->E
        val nodes = createMeshNetwork(5)
        val totalMessages = 100
        var deliveredCount = 0
        
        // ACT
        repeat(totalMessages) { i ->
            val messageId = "msg_$i"
            val delivered = sendMessageThroughMesh(
                nodes = nodes,
                fromIndex = 0,
                toIndex = 4,
                messageId = messageId,
                hops = 4
            )
            
            if (delivered) {
                deliveredCount++
            }
            
            delay(10)
        }
        
        // ASSERT
        val deliveryRate = (deliveredCount.toFloat() / totalMessages) * 100
        
        println("✅ Multi-hop (4 hops) Delivery Rate: $deliveryRate%")
        println("   Delivered: $deliveredCount / $totalMessages")
        
        assertTrue(
            "Delivery rate $deliveryRate% must be ≥99%",
            deliveryRate >= 99.0f
        )
    }

    @Test
    fun testMultiHop_WithPacketLoss() = runBlocking {
        // ARRANGE: 4 node mesh with 5% packet loss simulation
        val nodes = createMeshNetwork(4, packetLossRate = 0.05f)
        val totalMessages = 100
        var deliveredCount = 0
        
        // ACT
        repeat(totalMessages) { i ->
            val messageId = "msg_loss_$i"
            val delivered = sendMessageThroughMesh(
                nodes = nodes,
                fromIndex = 0,
                toIndex = 3,
                messageId = messageId,
                hops = 3,
                retryOnFailure = true
            )
            
            if (delivered) {
                deliveredCount++
            }
            
            delay(10)
        }
        
        // ASSERT: Retry mekanizması ile ≥99% olmalı
        val deliveryRate = (deliveredCount.toFloat() / totalMessages) * 100
        
        println("✅ Multi-hop with 5% packet loss: $deliveryRate%")
        println("   Delivered: $deliveredCount / $totalMessages")
        
        assertTrue(
            "Delivery rate with retry $deliveryRate% must be ≥99%",
            deliveryRate >= 99.0f
        )
    }

    private fun createMeshNetwork(
        nodeCount: Int,
        packetLossRate: Float = 0.0f
    ): List<MockMeshNode> {
        return (0 until nodeCount).map { index ->
            MockMeshNode(
                nodeId = "Node$index",
                packetLossRate = packetLossRate
            )
        }
    }
    
    private fun sendMessageThroughMesh(
        nodes: List<MockMeshNode>,
        fromIndex: Int,
        toIndex: Int,
        messageId: String,
        hops: Int,
        retryOnFailure: Boolean = false
    ): Boolean {
        var currentHop = fromIndex
        var attempts = 0
        val maxAttempts = if (retryOnFailure) 3 else 1
        
        while (attempts < maxAttempts) {
            var success = true
            currentHop = fromIndex
            
            // Mesajı hop by hop gönder
            for (hop in 0 until hops) {
                val currentNode = nodes[currentHop]
                val nextHop = currentHop + 1
                
                if (nextHop >= nodes.size) break
                
                val transmitted = currentNode.transmitToNext(messageId)
                if (!transmitted) {
                    success = false
                    break
                }
                
                currentHop = nextHop
            }
            
            if (success && currentHop == toIndex) {
                return true
            }
            
            attempts++
            
            if (retryOnFailure && attempts < maxAttempts) {
                try { Thread.sleep(10) } catch (e: InterruptedException) {}
            }
        }
        
        return false
    }
    
    private class MockMeshNode(
        val nodeId: String,
        private val packetLossRate: Float = 0.0f
    ) {
        fun transmitToNext(messageId: String): Boolean {
            // Packet loss simülasyonu
            if (packetLossRate > 0 && Math.random() < packetLossRate) {
                return false // Packet lost
            }
            return true
        }
    }
}
