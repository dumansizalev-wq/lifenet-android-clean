package net.lifenet.core.tor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStreamReader
import java.util.Scanner

class TorManager(private val context: Context) {
    private val TAG = "TorManager"
    private var torProcess: Process? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val torDir by lazy { File(context.filesDir, "tor_data") }
    private val torrcFile by lazy { File(torDir, "torrc") }
    private val cookieFile by lazy { File(torDir, "control_auth_cookie") }
    
    // Persistent Storage for HS Key
    private val prefs by lazy { context.getSharedPreferences("TOR_PREFS", Context.MODE_PRIVATE) }
    
    private val _onionAddress = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val onionAddress: kotlinx.coroutines.flow.StateFlow<String?> = _onionAddress

    fun startTor() {
        if (!torDir.exists()) torDir.mkdirs()
        
        scope.launch {
            try {
                // 1. Install/Locate Binary
                val nativeDir = context.applicationInfo.nativeLibraryDir
                val torBinary = File(nativeDir, "libtor.so")
                
                if (!torBinary.exists()) {
                    Log.e(TAG, "Tor binary not found at $nativeDir")
                    return@launch
                }

                // 2. Write Config
                writeTorrc()

                // 3. Start Process
                if (!torBinary.canExecute()) torBinary.setExecutable(true)

                val cmd = listOf(
                    torBinary.absolutePath,
                    "-f", torrcFile.absolutePath
                )
                
                Log.i(TAG, "Starting Tor: $cmd")
                val pb = ProcessBuilder(cmd)
                pb.environment()["HOME"] = torDir.absolutePath
                torProcess = pb.start()

                // Monitor Output
                launch { monitorStream(torProcess!!.inputStream) }
                launch { monitorStream(torProcess!!.errorStream) }

                // 4. Wait for Bootstrap and Control
                delay(5000) 
                setupHiddenService()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Tor", e)
            }
        }
    }

    private suspend fun setupHiddenService() {
        val control = TorControl(cookieFile)
        if (control.connectAndAuthenticate()) {
            val savedKey = prefs.getString("HS_KEY", null)
            val result = control.createHiddenService(12345, savedKey)
            
            if (result != null) {
                val address = result.first
                val newKey = result.second
                
                // Persist new key if generated
                if (savedKey == null && newKey.isNotEmpty()) {
                     prefs.edit().putString("HS_KEY", newKey).apply()
                     Log.i(TAG, "New HS Key persisted")
                }
                
                // Emit new address
                _onionAddress.emit(address)
                Log.i(TAG, "Tor HS Active: $address")
                control.close()
            }
        }
    }

    private fun writeTorrc() {
        val config = """
            DataDirectory ${torDir.absolutePath}
            ControlPort 9051
            CookieAuthentication 1
            CookieAuthFile ${cookieFile.absolutePath}
            SocksPort 9050
            # Reduced logging for production
            Log notice stdout
        """.trimIndent()
        torrcFile.writeText(config)
    }

    private fun monitorStream(stream: java.io.InputStream) {
        val scanner = Scanner(stream)
        while (scanner.hasNextLine()) {
            Log.d(TAG, "TOR: ${scanner.nextLine()}")
        }
    }
    
    fun stopTor() {
        // Optional: Clean removal of HS before kill (if we wanted to detach properly)
        // But process destroy is cleaner for mobile lifecycle.
        // We just clear the address state.
        _onionAddress.value = null
        torProcess?.destroy()
    }
}
