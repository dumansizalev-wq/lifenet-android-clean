package net.lifenet.core.messaging.assembler

import java.nio.ByteBuffer
import java.util.zip.CRC32

/**
 * LifenetFragmenter: Büyük paketleri MTU-uyumlu parçalara ayırır.
 * Header: [MessageID(8)|TotalChunks(2)|ChunkIndex(2)|IsFEC(1)|Checksum(4)]
 */
class LifenetFragmenter(private val mtuSize: Int = 512) {

    data class Fragment(
        val header: ByteArray,
        val data: ByteArray
    )

    fun fragmentate(messageId: Long, payload: ByteArray): List<Fragment> {
        val fragments = mutableListOf<Fragment>()
        val totalChunks = Math.ceil(payload.size.toDouble() / (mtuSize - 17)).toInt()

        for (i in 0 until totalChunks) {
            val start = i * (mtuSize - 17)
            val end = Math.min(start + (mtuSize - 17), payload.size)
            val chunkData = payload.sliceArray(start until end)

            val header = ByteBuffer.allocate(17)
                .putLong(messageId)
                .putShort(totalChunks.toShort())
                .putShort(i.toShort())
                .put(0.toByte()) // IsFEC = false
                
            val crc = CRC32()
            crc.update(chunkData)
            header.putInt(crc.value.toInt())

            fragments.add(Fragment(header.array(), chunkData))
        }

        return fragments
    }
}
