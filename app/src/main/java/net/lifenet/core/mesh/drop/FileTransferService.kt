package net.lifenet.core.mesh.drop

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket
import net.lifenet.core.R

class FileTransferService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1001, createNotification("Ready for Mesh Drop"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START_SERVER && !isRunning) {
            startServer()
        } else if (action == ACTION_SEND_FILE) {
            val host = intent.getStringExtra(EXTRA_HOST) ?: return START_NOT_STICKY
            val fileUri = intent.getParcelableExtra<Uri>(EXTRA_FILE_URI) ?: return START_NOT_STICKY
            sendFile(host, fileUri)
        }
        return START_NOT_STICKY
    }

    private fun startServer() {
        isRunning = true
        serviceScope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                Log.i(TAG, "Server started on port $PORT")
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    handleIncomingFile(client)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
            }
        }
    }

    private fun handleIncomingFile(socket: Socket) {
        serviceScope.launch {
            try {
                val input = socket.getInputStream()
                // Simple protocol: First 256 bytes for filename, next 8 for size
                val header = ByteArray(264)
                input.read(header)
                val fileName = String(header, 0, 256).trim()
                
                val downloadsDir = File(getExternalFilesDir(null), "MeshDrop")
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                
                val targetFile = File(downloadsDir, fileName)
                val output = FileOutputStream(targetFile)
                
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.close()
                socket.close()
                Log.i(TAG, "File received: $fileName")
            } catch (e: Exception) {
                Log.e(TAG, "Receive error", e)
            }
        }
    }

    private fun sendFile(host: String, uri: Uri) {
        serviceScope.launch {
            try {
                val socket = Socket(host, PORT)
                val output = socket.getOutputStream()
                
                val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return@launch
                val file = File(uri.path ?: "file") // simplified for skeleton
                
                // Header (filename)
                val header = ByteArray(264)
                System.arraycopy(file.name.take(255).toByteArray(), 0, header, 0, file.name.take(255).length)
                output.write(header)
                
                val input = FileInputStream(fileDescriptor.fileDescriptor)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                input.close()
                output.close()
                socket.close()
                Log.i(TAG, "File sent to $host")
            } catch (e: Exception) {
                Log.e(TAG, "Send error", e)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Mesh Drop", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(content: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("LIFENET Mesh Drop")
        .setContentText(content)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .build()

    override fun onDestroy() {
        isRunning = false
        serverSocket?.close()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "FileTransferService"
        private const val PORT = 8989
        private const val CHANNEL_ID = "mesh_drop_channel"
        
        const val ACTION_START_SERVER = "net.lifenet.core.action.START_SERVER"
        const val ACTION_SEND_FILE = "net.lifenet.core.action.SEND_FILE"
        const val EXTRA_HOST = "extra_host"
        const val EXTRA_FILE_URI = "extra_file_uri"
    }
}
