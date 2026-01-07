package net.lifenet.core.radio

/**
 * RadioStatsProvider: Interface for real-time radio metrics.
 * Provides raw data for congestion and health analysis.
 */
interface RadioStatsProvider {
    /**
     * @return Total packets sent by this node.
     */
    fun sentPackets(): Int

    /**
     * @return Total packets acknowledged by neighbors.
     */
    fun ackedPackets(): Int

    /**
     * @return List of recent RSSI samples (raw negative dBm values, e.g., -85).
     */
    fun recentRssiSamples(): List<Int>
}
