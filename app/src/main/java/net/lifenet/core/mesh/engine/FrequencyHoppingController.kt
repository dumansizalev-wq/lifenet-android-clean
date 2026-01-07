package net.lifenet.core.mesh.engine

import android.util.Log

/**
 * FrequencyHoppingController: Otonom ve deterministik kanal yönetimi.
 * STME skoruna ve çarpışma yoğunluğuna göre RF kanalını değiştirir.
 */
class FrequencyHoppingController(
    private val analyzer: MeshStateAnalyzer,
    private val minChannels: Int = 2,
    private val maxChannels: Int = 8
) {
    private var currentChannel: Int = 1
    private var lastSwitchEpoch: Long = 0
    private val EPOCH_DURATION_MS = 300000L // 5 Dakika (Ghost-Deletion uyumlu)

    fun evaluateHopping(): Int {
        val score = analyzer.currentCongestionScore()
        val currentTime = System.currentTimeMillis()

        // Epoch tabanlı ve sessiz geçiş zorunluluğu
        if (currentTime - lastSwitchEpoch < EPOCH_DURATION_MS) {
            return currentChannel
        }

        // STME Congestion Score + Collision Density bazlı karar
        if (score > 85) {
            val nextChannel = (currentChannel % maxChannels) + 1
            Log.i("LIFENET", "FH: Congestion High ($score). Hopping: CH $currentChannel -> CH $nextChannel")
            currentChannel = nextChannel
            lastSwitchEpoch = currentTime
        }

        return currentChannel
    }

    fun getCurrentChannel(): Int = currentChannel
}
