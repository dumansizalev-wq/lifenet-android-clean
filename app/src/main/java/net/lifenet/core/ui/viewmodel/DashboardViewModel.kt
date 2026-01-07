package net.lifenet.core.ui.viewmodel

import android.app.Application
import net.lifenet.core.data.DeliveryStatus
import net.lifenet.core.ui.Conversation
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.lifenet.core.telemetry.TelemetryData
import net.lifenet.core.data.Message
import net.lifenet.core.mesh.GhostRadioService
import net.lifenet.core.messaging.LifenetMessagingService
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import net.lifenet.core.ui.NearbyDevice
import net.lifenet.core.ui.DeviceAdapter
import net.lifenet.core.transport.vsie.VsieMessage
import android.os.Handler
import android.os.Looper
import java.util.UUID

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // Service Reference
    private var ghostService: GhostRadioService? = null
    private val messagingService = LifenetMessagingService(application)
    
    // LiveData State
    private val _telemetry = MutableLiveData<TelemetryData>()
    val telemetry: LiveData<TelemetryData> = _telemetry
    
    // Chat State
    private val _conversations = MutableLiveData<List<Conversation>>()
    val conversations: LiveData<List<Conversation>> = _conversations

    init {
        // Start observing messages immediately
        viewModelScope.launch {
             messagingService.getMessages().collect { messages ->
                 updateConversationPreview(messages)
             }
        }
    }

    private val _isAstraActive = MutableLiveData<Boolean>(false)
    val isAstraActive: LiveData<Boolean> = _isAstraActive
    
    // Toggles State
    private val _bleEnabled = MutableLiveData<Boolean>(true)
    val bleEnabled: LiveData<Boolean> = _bleEnabled
    
    private val _wifiEnabled = MutableLiveData<Boolean>(true)
    val wifiEnabled: LiveData<Boolean> = _wifiEnabled
    
    private val _torEnabled = MutableLiveData<Boolean>(false)
    val torEnabled: LiveData<Boolean> = _torEnabled
    
    // Service Binding Hook
    fun setService(service: GhostRadioService) {
        ghostService = service
        // Initialize state from service
        _bleEnabled.postValue(true) // Should get actual from service
        // Start flows
    }

    fun updateTelemetry(data: TelemetryData) {
        _telemetry.postValue(data)
    }

    fun setAstraMode(active: Boolean) {
        _isAstraActive.value = active
        // Logic to notify service
    }
    
    fun toggleBle(enabled: Boolean) {
        _bleEnabled.value = enabled
        ghostService?.setBleEnabled(enabled)
    }
    
    fun toggleWifi(enabled: Boolean) {
        _wifiEnabled.value = enabled
        ghostService?.setWifiEnabled(enabled)
    }
    
     fun toggleTor(enabled: Boolean) {
        _torEnabled.value = enabled
        ghostService?.setTorEnabled(enabled)
    }

    private fun updateConversationPreview(messages: List<Message>) {
        val grouped = messages.groupBy { if (it.isSentByMe) it.recipientId else it.senderId }
        val newConversations = grouped.map { (id, msgList) ->
            val lastMsg = msgList.maxByOrNull { it.timestamp }
            Conversation(
                contactId = id,
                contactName = "User $id",
                lastMessage = lastMsg?.content ?: "",
                timestamp = lastMsg?.timestamp ?: 0L,
                unreadCount = 0,
                isSentByMe = lastMsg?.isSentByMe ?: false,
                deliveryStatus = lastMsg?.deliveryStatus
            )
        }.sortedByDescending { it.timestamp }

        _conversations.postValue(newConversations)
    }

    // --- Nearby Devices Scanning ---
    private val _nearbyDevices = MutableLiveData<List<NearbyDevice>>()
    val nearbyDevices: LiveData<List<NearbyDevice>> = _nearbyDevices

    // VSIE Integration
    fun updateVsiePeers(messages: List<VsieMessage>) {
        val currentList = _nearbyDevices.value.orEmpty().toMutableList()
        
        // Remove old VSIE devices to refresh
        currentList.removeAll { it.type == "VSIE" }
        
        messages.forEach { msg ->
            // Check if exists
            if (currentList.none { it.address == msg.senderId }) {
                currentList.add(
                    NearbyDevice(
                        name = "VSIE Peer ${msg.senderId.take(4)}",
                        address = msg.senderId,
                        rssi = -50, // Mock RSSI for VSIE
                        type = "VSIE"
                    )
                )
            }
        }
        _nearbyDevices.postValue(currentList)
    }
    
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi
            val name = device.name ?: "Unknown Mesh Node"
            // Filter for LIFENET nodes or show all for demo
            val found = NearbyDevice(name, device.address, rssi, "BLE")
            
            val currentList = _nearbyDevices.value.orEmpty().toMutableList()
            // Update or Add
            val index = currentList.indexOfFirst { it.address == found.address }
            if (index != -1) {
                currentList[index] = found
            } else {
                currentList.add(found)
            }
            _nearbyDevices.postValue(currentList)
        }
    }

    fun startScanning() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
            // Mock Bridge for Demo if no real devices found
            Handler(Looper.getMainLooper()).postDelayed({
                if (_nearbyDevices.value.isNullOrEmpty()) {
                    _nearbyDevices.postValue(listOf(
                        NearbyDevice("LIFENET-RELAY-01", "00:11:22:AA:BB:CC", -45, "BLE-MESH"),
                        NearbyDevice("ASTRA-HEADQUARTERS", "AA:BB:CC:DD:EE:FF", -88, "TOR-BRIDGE")
                    ))
                }
            }, 3000)
        } catch (e: SecurityException) {
            // Log error
        }
    }
    
    fun stopScanning() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            // Log error
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopScanning()
    }
}

