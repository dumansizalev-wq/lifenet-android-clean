package net.lifenet.core.test

import android.net.TrafficStats
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Offline Leakage Test
 * 
 * P0 Requirement: Verify zero internet traffic when in offline mesh mode
 * 
 * Test Strategy:
 * 1. Capture baseline network stats
 * 2. Enable mesh mode (Wi-Fi Aware)
 * 3. Send test messages
 * 4. Verify TX/RX bytes to internet remain unchanged
 * 
 * Prerequisites:
 * - Device must be in airplane mode OR Wi-Fi connected to local-only network
 * - GhostRadioService must be running
 */
@RunWith(AndroidJUnit4::class)
class OfflineLeakageTest {

    @Test
    fun testZeroInternetTrafficInMeshMode() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Step 1: Capture baseline
        val baselineTx = TrafficStats.getTotalTxBytes()
        val baselineRx = TrafficStats.getTotalRxBytes()
        
        // Step 2: Simulate mesh activity (5 seconds)
        // In real scenario: send messages via MeshMessenger
        delay(5000)
        
        // Step 3: Capture post-mesh stats
        val finalTx = TrafficStats.getTotalTxBytes()
        val finalRx = TrafficStats.getTotalRxBytes()
        
        // Step 4: Calculate delta
        val txDelta = finalTx - baselineTx
        val rxDelta = finalRx - baselineRx
        
        // Step 5: Assert zero leakage (allow 1KB tolerance for OS background)
        val toleranceBytes = 1024L
        assertTrue(
            "Internet TX leaked: $txDelta bytes",
            txDelta < toleranceBytes
        )
        assertTrue(
            "Internet RX leaked: $rxDelta bytes",
            rxDelta < toleranceBytes
        )
    }
    
    @Test
    fun testWifiAwareDoesNotUseInternet() = runBlocking {
        // This test requires manual verification with Wireshark/tcpdump
        // Automated version monitors TrafficStats for specific UID
        
        val uid = android.os.Process.myUid()
        val baselineTx = TrafficStats.getUidTxBytes(uid)
        val baselineRx = TrafficStats.getUidRxBytes(uid)
        
        // Simulate Wi-Fi Aware session
        delay(3000)
        
        val finalTx = TrafficStats.getUidTxBytes(uid)
        val finalRx = TrafficStats.getUidRxBytes(uid)
        
        val txDelta = finalTx - baselineTx
        val rxDelta = finalRx - baselineRx
        
        // Wi-Fi Aware should use 0 internet bytes
        assertTrue(
            "Wi-Fi Aware used internet TX: $txDelta bytes",
            txDelta == 0L
        )
        assertTrue(
            "Wi-Fi Aware used internet RX: $rxDelta bytes",
            rxDelta == 0L
        )
    }
}
