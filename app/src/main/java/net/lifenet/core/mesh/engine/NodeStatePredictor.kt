package net.lifenet.core.mesh.engine

/**
 * NodeStatePredictor:
 * Düğüm durumlarını tahmin ederek sync trafiğini minimize eder.
 * Sliding window history ve congestion score modellemesi kullanır.
 */
class NodeStatePredictor {

    private val peerHistory = mutableMapOf<String, MutableList<PeerState>>()
    private val WINDOW_SIZE = 10

    data class PeerState(
        val timestamp: Long,
        val congestionScore: Int,
        val signalStrength: Int
    )

    /**
     * Peer durumunu geçmişe kaydeder.
     */
    fun updatePeerHistory(peerId: String, state: PeerState) {
        val history = peerHistory.getOrPut(peerId) { mutableListOf() }
        history.add(state)
        if (history.size > WINDOW_SIZE) {
            history.removeAt(0)
        }
    }

    /**
     * Mevcut verilere dayanarak peer'ın bir sonraki durumunu tahmin eder.
     * true  -> sync GEREKLİ
     * false -> sync ERTELENEBİLİR
     */
    fun shouldSync(peerId: String, currentScore: Int): Boolean {
        val history = peerHistory[peerId] ?: return true
        if (history.size < 3) return true

        val recent = history.takeLast(3)
        val avgScore = recent.map { it.congestionScore }.average()
        val variance = recent
            .map { (it.congestionScore - avgScore) * (it.congestionScore - avgScore) }
            .average()

        return variance > 5.0 || kotlin.math.abs(currentScore - avgScore) > 10
    }
}
