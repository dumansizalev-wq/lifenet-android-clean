package net.lifenet.core.radio

import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import net.lifenet.core.mode.LifenetMode

/**
 * WifiDirectManager: Yüksek bant genişliği (Ses/Dosya) için P2P bağlantı yönetimi.
 * Mode-Aware: DISASTER modunda agresif peer discovery, ASTRA modunda pasif.
 */
class WifiDirectManager(private val context: Context) {
    private val manager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }
    private val channel: WifiP2pManager.Channel? by lazy {
        manager?.initialize(context, context.mainLooper, null)
    }
    
    private var currentMode: LifenetMode = LifenetMode.DAILY

    /**
     * Wi-Fi Direct üzerinden keşif başlatır.
     */
    fun discoverPeers() {
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i("LIFENET", "WF-Direct: Peer discovery initiated (Mode: $currentMode)")
            }

            override fun onFailure(reason: Int) {
                Log.e("LIFENET", "WF-Direct: Discovery failed (Reason: $reason)")
            }
        })
    }
    
    /**
     * Mod değişikliğine göre discovery davranışını günceller.
     * DISASTER: Agresif, sürekli discovery
     * ASTRA: Pasif, talep bazlı discovery
     */
    fun updateDiscoveryMode(mode: LifenetMode) {
        currentMode = mode
        Log.i("LIFENET", "WF-Direct: Discovery mode updated to ${if (mode == LifenetMode.DISASTER) "AGGRESSIVE" else "PASSIVE"}")
        
        // DISASTER modunda discovery'yi yeniden tetikle
        if (mode == LifenetMode.DISASTER) {
            discoverPeers()
        }
    }

    /**
     * Belirli bir düğüme bağlanır.
     */
    fun connectToPeer(deviceAddress: String) {
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
        }

        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i("LIFENET", "WF-Direct: Connecting to $deviceAddress...")
            }

            override fun onFailure(reason: Int) {
                Log.e("LIFENET", "WF-Direct: Connection failed (Reason: $reason)")
            }
        })
    }

    /**
     * Veri transferi için P2P soketlerini hazırlar (Placeholder).
     */
    fun initiateTransfer(payload: ByteArray) {
        Log.i("LIFENET", "WF-Direct: Initiating high-speed transfer (${payload.size} bytes)")
        // Soket bazlı veri transferi burada gerçekleşir...
    }
}
