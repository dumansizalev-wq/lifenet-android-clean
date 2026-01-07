package net.lifenet.core.mesh.engine

import android.content.Context
import android.net.wifi.aware.PeerHandle
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Decides which transport layer to use based on signal quality and peer availability.
 * Strategy:
 * 1. Default: Wi-Fi Aware (Passive/Low Power)
 * 2. If RSSI < -75dBm OR Large Payload: PROPOSE Wi-Fi Direct
 * 3. If Direct Fails: PROPOSE SoftAP
 */
class MeshDecisionEngine(private val context: Context) {

    private val TAG = "MeshDecisionEngine"

    enum class TransportMode {
        AWARE_NAN,
        WIFI_DIRECT,
        SOFT_AP
    }

    private val _currentMode = MutableStateFlow(TransportMode.AWARE_NAN)
    val currentMode: StateFlow<TransportMode> = _currentMode

    fun analyzeSignal(peerHandle: PeerHandle, rssi: Int, distanceMm: Int) {
        // Simple Heuristic for Emergency Release
        Log.d(TAG, "Analyzing Signal: RSSI=$rssi, Dist=$distanceMm")

        if (rssi < -75 && rssi != 0) { // 0 often means unknown
            Log.i(TAG, "Signal Weak ($rssi dBm). Recommending upgrade to Wi-Fi Direct/P2P for stability.")
            // Ideally we initiate P2P here. For today's release, we log it and verify 'Aware' works first.
            // _currentMode.value = TransportMode.WIFI_DIRECT 
        } else {
             _currentMode.value = TransportMode.AWARE_NAN
        }
    }

    fun recommendTransportForPayload(sizeBytes: Int): TransportMode {
        return if (sizeBytes > 255) {
            TransportMode.WIFI_DIRECT // Aware cannot handle > 255 bytes easily without fragmentation
        } else {
            TransportMode.AWARE_NAN
        }
    }
}
