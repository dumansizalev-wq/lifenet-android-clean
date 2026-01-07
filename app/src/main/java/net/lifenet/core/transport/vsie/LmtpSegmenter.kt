package net.lifenet.core.transport.vsie

class LmtpSegmenter(private val segmentSize: Int = 255) {

    fun fragment(payload: ByteArray): List<ByteArray> {
        val segments = mutableListOf<ByteArray>()
        var index = 0
        while (index < payload.size) {
            val end = (index + segmentSize).coerceAtMost(payload.size)
            segments.add(payload.sliceArray(index until end))
            index = end
        }
        return segments
    }

    fun reassemble(fragmented: ByteArray): List<ByteArray> {
        // In a real scenario, track fragment IDs & order. Here we assume one message per call
        return listOf(fragmented)
    }
}
