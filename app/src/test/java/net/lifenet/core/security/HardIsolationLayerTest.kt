package net.lifenet.core.security

import android.content.Context
import net.lifenet.core.BuildConfig
import net.lifenet.core.messaging.MessageEnvelope
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class HardIsolationLayerTest {
    
    @Mock
    private lateinit var context: Context
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }
    
    @Test
    fun testB2cIsolation_PublicMesh() {
        // Build config simülasyonu (Reflection veya Mock gerekebilir, 
        // ancak Robolectric ile BuildConfig manipülasyonu zordur.
        // Bu yüzden strateji sınıfını doğrudan test ediyoruz.)
        
        val strategy = PublicMeshIsolation()
        
        // B2C her şeyi kabul etmeli
        assertTrue(strategy.canAcceptPeer("any-peer-id"))
        
        val envelope = MessageEnvelope(
             id = "msg1",
             senderId = "sender1",
             targetId = "target1",
             payload = ByteArray(0),
             timestamp = System.currentTimeMillis(),
             lastHopNodeId = "hop1",
             hopCount = 0
        )
        assertTrue(strategy.canRelayMessage(envelope))
    }
    
    @Test
    fun testEnterpriseIsolation_PrivateMesh() {
        val allowedPeers = setOf("trusted-peer-1", "trusted-peer-2")
        val strategy = PrivateMeshIsolation(allowedPeers)
        
        // İzin verilen peer'lar
        assertTrue(strategy.canAcceptPeer("trusted-peer-1"))
        assertTrue(strategy.canAcceptPeer("trusted-peer-2"))
        
        // Bilinmeyen peer
        assertFalse(strategy.canAcceptPeer("unknown-peer"))
        
        // Mesaj röle testi - İzin verilenler arası
        val trustedMsg = MessageEnvelope(
             id = "msg2",
             senderId = "trusted-peer-1",
             targetId = "trusted-peer-2",
             payload = ByteArray(0),
             timestamp = System.currentTimeMillis(),
             lastHopNodeId = "hop1",
             hopCount = 0
        )
        assertTrue(strategy.canRelayMessage(trustedMsg))
        
        // Mesaj röle testi - Bilinmeyen gönderen
        val untrustedSenderMsg = MessageEnvelope(
             id = "msg3",
             senderId = "unknown-peer",
             targetId = "trusted-peer-1",
             payload = ByteArray(0),
             timestamp = System.currentTimeMillis(),
             lastHopNodeId = "hop1",
             hopCount = 0
        )
        assertFalse(strategy.canRelayMessage(untrustedSenderMsg))
        
        // Mesaj röle testi - Bilinmeyen alıcı
        val untrustedTargetMsg = MessageEnvelope(
             id = "msg4",
             senderId = "trusted-peer-1",
             targetId = "unknown-peer",
             payload = ByteArray(0),
             timestamp = System.currentTimeMillis(),
             lastHopNodeId = "hop1",
             hopCount = 0
        )
        assertFalse(strategy.canRelayMessage(untrustedTargetMsg))
    }
}
