package net.lifenet.core.mesh.transport

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log

/**
 * BleTransport: Neighbor discovery and low-bandwidth transport.
 * Modernize edildi ve Context bağımlılığı eklendi.
 */
class BleTransport(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }
    
    private var isStarted = false

    fun isStarted(): Boolean = isStarted

    fun startAdvertising(nodeId: String) {
        isStarted = true
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: run {
            Log.e("LIFENET", "BLE Advertiser kullanılamıyor (Bluetooth kapalı mı?)")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceData(
                ParcelUuid.fromString("0000180a-0000-1000-8000-00805f9b34fb"), 
                nodeId.toByteArray(Charsets.UTF_8)
            )
            .build()

        try {
            advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    Log.i("LIFENET", "BLE Advertising started: $nodeId")
                }

                override fun onStartFailure(errorCode: Int) {
                    Log.e("LIFENET", "BLE Advertising başlatılamadı, hata kodu: $errorCode")
                }
            })
        } catch (e: SecurityException) {
            Log.e("LIFENET", "Bluetooth izinleri (Advertise) eksik: ${e.message}")
        }
    }
    
    fun stop() {
        // Tarama ve reklam durdurma mantığı
    }

    fun getPeerCount(): Int = 0 // Mock impl
}
