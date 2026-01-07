package net.lifenet.core.voice

// Imports inferred from user snippet and dependency org.gagravarr:opus
import org.gagravarr.opus.OpusEncoder
import org.gagravarr.opus.OpusDecoder
import org.gagravarr.opus.OpusApplication

class OpusCodec {
    private val encoder = OpusEncoder(16000, 1, OpusApplication.OPUS_APPLICATION_VOIP)
    private val decoder = OpusDecoder(16000, 1)

    fun encode(pcm: ShortArray): ByteArray {
        val out = ByteArray(400) // Max frame size safe buffer
        val len = encoder.encode(pcm, 0, pcm.size, out, 0, out.size)
        return out.copyOf(len)
    }

    fun decode(data: ByteArray): ShortArray {
        val pcm = ShortArray(320) // 20ms at 16kHz
        decoder.decode(data, 0, data.size, pcm, 0, false)
        return pcm
    }

    fun close() {
        // encoder.close() // If disposable
        // decoder.close()
    }
}
