package net.lifenet.core.radio

import android.content.Context
import android.util.Log
import net.lifenet.core.messaging.assembler.LifenetFragmenter
import net.lifenet.core.messaging.assembler.LifenetAssembler
import net.lifenet.core.messaging.assembler.ForwardErrorCorrection
import java.nio.ByteBuffer

/**
 * HybridTransportManager: BLE ve Wi-Fi Direct donanımları arasında orkestrasyon sağlar.
 * Signaling için BLE, yüksek hacimli veri için Wi-Fi Direct kullanır.
 */
class HybridTransportManager(
    private val context: Context,
    private val nodeId: String,
    private val bleController: BluetoothLeController,
    private val wifiDirectManager: WifiDirectManager
) {
    private val PAYLOAD_THRESHOLD = 512
    private val fragmenter = LifenetFragmenter(PAYLOAD_THRESHOLD)
    private val fecEngine = ForwardErrorCorrection()
    private val assembler = LifenetAssembler(fecEngine)

    /**
     * Tüm donanım katmanlarını başlatır.
     */
    fun start() {
        Log.i("LIFENET", "HybridTransport: Initializing hardware stack...")
        bleController.startAdvertising()
        bleController.startScanning()
    }

    /**
     * Mesaj tipine ve boyutuna göre en uygun donanım yolunu seçer.
     * Future Optimization: Büyük paketler fragmanlara ayrılır ve FEC eklenir.
     */
    fun sendPacket(packetType: Byte, payload: ByteArray, targetNodeId: String?) {
        if (payload.size > PAYLOAD_THRESHOLD || packetType == 0x01.toByte()) {
            val messageId = System.currentTimeMillis() // Simplistic ID
            val fragments = fragmenter.fragmentate(messageId, payload)
            
            // Fragmanları Gönder
            fragments.forEach { frag ->
                wifiDirectManager.initiateTransfer(frag.header + frag.data)
            }

            // Anayasal Madde: Zero-Ack FEC Parity Chunk Gönder
            val parity = fecEngine.generateParity(messageId, fragments.map { it.data })
            val fecHeader = fragments[0].header.copyOf()
            fecHeader[12] = 1.toByte() // IsFEC = true
            wifiDirectManager.initiateTransfer(fecHeader + parity)
            
            Log.d("LIFENET", "HybridTransport: Large Payload ($messageId) Fragmented into ${fragments.size}+1 chunks")
        } else {
            bleController.startAdvertising() // Mock send
        }
    }

    /**
     * Düşük seviyeli radyodan gelen verileri toplar ve birleştirir.
     */
    fun onDataReceived(data: ByteArray) {
        val buffer = ByteBuffer.wrap(data)
        if (data.size < 17) return

        val messageId = buffer.getLong()
        val totalChunks = buffer.getShort().toInt()
        val chunkIndex = buffer.getShort().toInt()
        val isFEC = buffer.get() == 1.toByte()
        buffer.getInt() // Skip checksum for now
        
        val chunkData = ByteArray(buffer.remaining())
        buffer.get(chunkData)

        val assembled = assembler.onFragmentReceived(messageId, totalChunks, chunkIndex, isFEC, chunkData)
        if (assembled != null) {
            Log.i("LIFENET", "HybridTransport: Full Message Reassembled. Pushing to stack.")
            // Çekirdek işleme katmanına ilet...
        }
    }

    /**
     * Donanım katmanlarını kapatır.
     */
    fun stop() {
        bleController.stopAll()
        // Wi-Fi Direct cleanup...
    }
}
