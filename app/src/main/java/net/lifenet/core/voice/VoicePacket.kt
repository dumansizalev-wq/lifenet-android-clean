package net.lifenet.core.voice

data class VoicePacket(
    val streamId: String,
    val seq: Int,
    val ttl: Int,
    val hop: Int,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoicePacket

        if (streamId != other.streamId) return false
        if (seq != other.seq) return false
        if (ttl != other.ttl) return false
        if (hop != other.hop) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = streamId.hashCode()
        result = 31 * result + seq
        result = 31 * result + ttl
        result = 31 * result + hop
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
