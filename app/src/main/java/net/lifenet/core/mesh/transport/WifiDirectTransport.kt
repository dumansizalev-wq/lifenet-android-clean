package net.lifenet.core.mesh.transport

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log

/**
 * WifiDirectTransport: High-bandwidth burst transport.
 */
class WifiDirectTransport(context: Context) {

    private val manager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: WifiP2pManager.Channel? = manager?.initialize(context, context.mainLooper, null)

    fun discoverPeers() {
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("LIFENET", "Wi-Fi Direct: Peer discovery started")
            }

            override fun onFailure(reason: Int) {
                Log.e("LIFENET", "Wi-Fi Direct: Peer discovery failed ($reason)")
            }
        })
    }
}
