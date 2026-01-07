package net.lifenet.core.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.lifenet.core.astra.ASTRAController
import net.lifenet.core.emergency.SOSService
import net.lifenet.core.mesh.GhostRadioService
import net.lifenet.core.telemetry.TelemetryCollector
import net.lifenet.core.ui.theme.LIFENETTheme
import net.lifenet.core.ui.viewmodel.DashboardViewModel
import net.lifenet.core.utils.AlertHardener
import net.lifenet.core.utils.PersistenceManager

class DashboardActivity : ComponentActivity() {
    private lateinit var telemetryCollector: TelemetryCollector
    private lateinit var astraController: ASTRAController
    private lateinit var sosService: SOSService
    private lateinit var alertHardener: AlertHardener
    private lateinit var persistenceManager: PersistenceManager
    private lateinit var vsieManager: net.lifenet.core.transport.vsie.VsieManager
    
    // V5.2 Hybrid Engines
    private lateinit var modeController: net.lifenet.core.controller.ModeController
    private lateinit var torVoiceEngine: net.lifenet.core.voice.TorVoiceEngine
    private lateinit var hopVoiceEngine: net.lifenet.core.voice.HopVoiceEngine
    private lateinit var contactEngine: net.lifenet.core.contact.ContactEngine
    private lateinit var torManager: net.lifenet.core.tor.TorManager
    
    // Service Binding
    private var ghostService: GhostRadioService? = null
    private var isBound = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions Denied", Toast.LENGTH_LONG).show()
        }
    }

    private val viewModel: DashboardViewModel by viewModels()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as GhostRadioService.GhostBinder
            ghostService = binder.getService()
            isBound = true
            
            // Link ViewModel
            viewModel.setService(binder.getService())
            
            telemetryCollector.attachService(binder.getService())
            telemetryCollector.startCollection()
            
            // Observe Telemetry Flow and feed ViewModel
            lifecycleScope.launch {
                telemetryCollector.telemetryFlow.collect { data ->
                    viewModel.updateTelemetry(data)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            ghostService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        telemetryCollector = TelemetryCollector(this)
        astraController = ASTRAController(this)
        sosService = SOSService(this)
        alertHardener = AlertHardener(this)
        persistenceManager = PersistenceManager(this)
        vsieManager = net.lifenet.core.transport.vsie.VsieManager(this)
        
        // Initialize Hybrid Engines
        torManager = net.lifenet.core.tor.TorManager(this)
        modeController = net.lifenet.core.controller.ModeController()
        torVoiceEngine = net.lifenet.core.voice.TorVoiceEngine(torManager)
        hopVoiceEngine = net.lifenet.core.voice.HopVoiceEngine(vsieManager, lifecycleScope)
        contactEngine = net.lifenet.core.contact.ContactEngine(this)
        
        // Start Tor Background Process
        torManager.startTor()
        
        // WIRE RECEIVER: Route Voice Packets to Engine
        vsieManager.voiceListener = { packet ->
            hopVoiceEngine.onReceive(packet)
        }
        
        // Setup Compose UI
        setContent {
            LIFENETTheme {
                val mode by modeController.mode.collectAsState()
                val vsieMessages by vsieManager.discoveredMessages.collectAsState()
                val onionAddr by torManager.onionAddress.collectAsState(initial = null)
                
                net.lifenet.core.ui.screens.LifeneDashboard(
                    mode = mode,
                    peerCount = vsieMessages.size,
                    onionAddress = onionAddr,
                    onToggle = { isAstra -> 
                        if (isAstra) {
                            modeController.switchToAstra()
                            // Update systems
                        } else {
                            modeController.switchToHop()
                        }
                    },
                    onStartTorCall = {
                         // Hardcoded peer for demo/verification logic if needed, or prompt user.
                         // For verification step "Click Start Secure Call", we use a test onion or prompts.
                         // Assuming verifying connectivity:
                         torVoiceEngine.startCall("v2testpeer.onion") {
                             // Failover Callback (Runs on failure)
                             runOnUiThread {
                                 android.widget.Toast.makeText(this@DashboardActivity, "Tor Call Failed - Switching to Mesh", android.widget.Toast.LENGTH_SHORT).show()
                                 modeController.switchToHop()
                             }
                         }
                    }
                )
                
                // Show consent dialog on top if needed
                LifenetApp(
                    persistenceManager = persistenceManager,
                    onConsentGranted = { onVsieConsentGranted() }
                )
            }
        }

        startAndBindService()
        checkPermissions()
        
        // Universal VSIE: Only start if consent is already granted
        if (persistenceManager.isVsieConsentGranted()) {
            vsieManager.start()
        }
        
        // Observe VSIE peers and feed ViewModel
        lifecycleScope.launch {
            vsieManager.discoveredMessages.collect { messages ->
                viewModel.updateVsiePeers(messages)
            }
        }
        
        // Bind MeshMessenger for persistence
        net.lifenet.core.messaging.MeshMessenger.bindVsieManager(vsieManager, lifecycleScope)
    }
    
    // Called from UI when consent dialog is accepted
    fun onVsieConsentGranted() {
        vsieManager.start()
        Toast.makeText(this, "Evrensel Sinyal Sörfü Aktif 🌊", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_CONTACTS)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.RECORD_AUDIO)
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.SEND_SMS)
        
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 2000)
        }
    }

    private fun startAndBindService() {
        val serviceIntent = Intent(this, GhostRadioService::class.java)
        startForegroundService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /* Old ScanResult Receiver Removed - Replaced by Wi-Fi Aware VsieManager */
    
    /* setupVsieScanner Removed */

    override fun onDestroy() {
        super.onDestroy()
        alertHardener.shutdown()
        telemetryCollector.stopCollection()
        vsieManager.stop()
        torVoiceEngine.disconnect()
        hopVoiceEngine.stopTransmit()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
    
    companion object {
        private const val TAG = "DashboardActivity"
    }
}
