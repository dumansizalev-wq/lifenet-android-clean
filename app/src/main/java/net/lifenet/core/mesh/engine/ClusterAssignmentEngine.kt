package net.lifenet.core.mesh.engine

import kotlin.math.abs

/**
 * ClusterAssignmentEngine: Düğümleri deterministik olarak cluster'lara atar.
 */
class ClusterAssignmentEngine {

    /**
     * NodeId ve mevcut kanal sayısına göre hedef kanalı belirler.
     * @param nodeId Düğümün benzersiz kimliği
     * @param totalChannels Mevcut aktif kanal sayısı
     * @return Atanan kanal numarası (1-indexed)
     */
    fun getAssignedChannel(nodeId: String, totalChannels: Int): Int {
        if (totalChannels <= 1) return 1
        
        // Deterministik hash bazlı atama
        val hash = nodeId.hashCode()
        return (abs(hash) % totalChannels) + 1
    }

    /**
     * Düğümün kendi cluster'ındaki peer'ları takip edip etmeyeceğine karar verir.
     */
    fun isPeerInMyCluster(myNodeId: String, peerNodeId: String, totalChannels: Int): Boolean {
        val myCluster = getAssignedChannel(myNodeId, totalChannels)
        val peerCluster = getAssignedChannel(peerNodeId, totalChannels)
        return myCluster == peerCluster
    }
}
