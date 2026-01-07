package net.lifenet.core.radio

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.os.ParcelUuid
import android.util.Log
import net.lifenet.core.mode.LifenetMode
import java.util.*

/**
 * BluetoothLeController: BLE tabanlı sürekli keşif ve beaconing sistemi.
 * LIFENET 'TYPE_BEACON' (0x07) paketlerini yönetir.
 * Mode-Aware: DISASTER modunda agresif, ASTRA modunda enerji tasarruflu tarama.
 */
class BluetoothLeController(
    private val adapter: BluetoothAdapter?,
    private val nodeId: String
) {
    private val SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB") // Mock UUID
    private val advertiser: BluetoothLeAdvertiser? = adapter?.bluetoothLeAdvertiser
    private val scanner: BluetoothLeScanner? = adapter?.bluetoothLeScanner
    
    private var currentScanMode: Int = ScanSettings.SCAN_MODE_LOW_POWER
    private var isScanning: Boolean = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name ?: "Unknown"
            Log.d("LIFENET", "BLE Discovery: Found peer $deviceName (RSSI: ${result.rssi})")
            // Keşfedilen peer'ı MeshEngine'e ilet...
        }
    }

    /**
     * BLE Beaconing başlatır. Diğer düğümlerin bizi keşfetmesini sağlar.
     */
    fun startAdvertising() {
        if (advertiser == null) return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.i("LIFENET", "BLE: Advertising started successfully (Node: $nodeId)")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e("LIFENET", "BLE: Advertising failed (Error: $errorCode)")
            }
        })
    }

    /**
     * Yakındaki düğümleri taramaya başlar.
     */
    fun startScanning() {
        if (scanner == null) return

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(currentScanMode)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
        isScanning = true
        Log.i("LIFENET", "BLE: Scanning initiated with mode: $currentScanMode")
    }
    
    /**
     * Mod değişikliğine göre tarama agresifliğini günceller.
     * DISASTER: LOW_LATENCY (Yüksek enerji, hızlı keşif)
     * DAILY: LOW_POWER (Düşük enerji, yavaş keşif)
     */
    fun updateScanMode(mode: LifenetMode) {
        val newScanMode = when (mode) {
            LifenetMode.DISASTER -> ScanSettings.SCAN_MODE_LOW_LATENCY
            LifenetMode.DAILY -> ScanSettings.SCAN_MODE_LOW_POWER
        }
        
        if (newScanMode != currentScanMode) {
            currentScanMode = newScanMode
            Log.i("LIFENET", "BLE: Scan mode updated to ${if (mode == LifenetMode.DISASTER) "AGGRESSIVE" else "CONSERVATIVE"}")
            
            // Tarama aktifse yeniden başlat
            if (isScanning) {
                stopScanning()
                startScanning()
            }
        }
    }
    
    private fun stopScanning() {
        scanner?.stopScan(scanCallback)
        isScanning = false
    }

    fun stopAll() {
        advertiser?.stopAdvertising(object : AdvertiseCallback() {})
        stopScanning()
    }
}
