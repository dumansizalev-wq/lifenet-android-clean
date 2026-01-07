package net.lifenet.core.tor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Socket
import java.util.Scanner

class TorControl(private val cookieFile: File) {
    private val TAG = "TorControl"
    private var socket: Socket? = null

    suspend fun connectAndAuthenticate(): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = Socket("127.0.0.1", 9051)
            val scanner = Scanner(socket!!.getInputStream())
            val writer = socket!!.getOutputStream()

            // Protocol requires authentication
            // Read cookie
            if (!cookieFile.exists()) {
                Log.e(TAG, "Cookie file missing: ${cookieFile.absolutePath}")
                return@withContext false
            }
            val cookieBytes = cookieFile.readBytes()
            val hexCookie = bytesToHex(cookieBytes)

            // Send AUTH
            writer.write("AUTH $hexCookie\r\n".toByteArray())
            writer.flush()

            val response = scanner.nextLine()
            if (response.startsWith("250")) {
                Log.i(TAG, "Tor Control Authenticated")
                return@withContext true
            } else {
                Log.e(TAG, "Auth failed: $response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Control connection failed", e)
        }
        return@withContext false
    }

    suspend fun createHiddenService(targetPort: Int, privateKey: String?): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val writer = socket?.getOutputStream() ?: return@withContext null
            val scanner = Scanner(socket!!.getInputStream())

            val keyParam = privateKey ?: "NEW:ED25519-V3" // KeyType:KeyBlob or NEW
            
            // ADD_ONION KeyPort Port=TargetPort
            // Using "Flags=Detach" to keep it alive if control disconnects
            val command = "ADD_ONION $keyParam Port=$targetPort,$targetPort Flags=Detach\r\n"
            writer.write(command.toByteArray())
            writer.flush()

            // Parse Multi-line response
            // 250-ServiceID=...
            // 250-PrivateKey=...
            // 250 OK
            
            var serviceId: String? = null
            var newPrivateKey: String? = null
            
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                Log.d(TAG, "Control: $line")
                if (line.startsWith("250-ServiceID=")) {
                    serviceId = line.substringAfter("=")
                } else if (line.startsWith("250-PrivateKey=")) {
                    newPrivateKey = line.substringAfter("=")
                } else if (line.startsWith("250 OK")) {
                    break
                } else if (line.startsWith("5")) {
                    Log.e(TAG, "ADD_ONION failed: $line")
                    return@withContext null
                }
            }

            if (serviceId != null) {
                // If we used an existing key, PrivateKey might not be returned, return the one we passed
                val key = newPrivateKey ?: privateKey ?: "" 
                return@withContext Pair("$serviceId.onion", key)
            }

        } catch (e: Exception) {
            Log.e(TAG, "HS creation failed", e)
        }
        return@withContext null
    }

    suspend fun destroyHiddenService(serviceId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val writer = socket?.getOutputStream() ?: return@withContext false
            val scanner = Scanner(socket!!.getInputStream())
            
            // Extract ID if full onion address provided
            val cleanId = serviceId.removeSuffix(".onion")
            
            val command = "DEL_ONION $cleanId\r\n"
            writer.write(command.toByteArray())
            writer.flush()
            
            val response = scanner.nextLine()
            if (response.startsWith("250")) {
                Log.i(TAG, "DEL_ONION success: $cleanId")
                return@withContext true
            } else {
                 Log.e(TAG, "DEL_ONION failed: $response")
            }
        } catch (e: Exception) {
             Log.e(TAG, "HS destruction failed", e)
        }
        return@withContext false
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    fun close() {
        socket?.close()
    }
}
