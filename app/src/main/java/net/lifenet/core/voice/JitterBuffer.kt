package net.lifenet.core.voice

import java.util.PriorityQueue

class JitterBuffer(
    private val capacityMs: Int = 300, 
    private val frameDurationMs: Int = 20
) {
    // 300ms / 20ms = ~15 packets
    private val targetSize = capacityMs / frameDurationMs
    private val buffer = PriorityQueue<VoicePacket> { a, b -> a.seq - b.seq }
    private var lastSeq = -1
    private var isBuffering = true

    fun push(packet: VoicePacket) {
        if (packet.seq > lastSeq) {
            buffer.add(packet)
            // Drop manual check if buffer explodes
            if (buffer.size > targetSize * 2) {
                 // Overflow protection: Drop oldest or wait reset
                 buffer.poll() // Drop oldest
            }
        }
    }

    fun pop(): VoicePacket? {
        if (isBuffering) {
            if (buffer.size >= targetSize) {
                isBuffering = false
                return buffer.poll()?.also { lastSeq = it.seq }
            }
            return null
        }
        
        if (buffer.isEmpty()) {
            isBuffering = true
            return null
        }
        
        // Return next packet
        return buffer.poll()?.also { lastSeq = it.seq }
    }
}
