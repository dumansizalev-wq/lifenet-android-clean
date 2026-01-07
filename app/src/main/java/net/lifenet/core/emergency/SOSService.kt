package net.lifenet.core.emergency

import android.content.Context
import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import net.lifenet.core.data.AlertPriority
import net.lifenet.core.data.LifenetDatabase
import net.lifenet.core.data.SOSAlert
import net.lifenet.core.mesh.GhostRadioService

class SOSService(private val context: Context) {
    
    private val database = LifenetDatabase.getInstance(context)
    private val sosAlertDao = database.sosAlertDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val identityManager = net.lifenet.core.identity.IdentityManager(context)
    
    // Get active SOS alerts
    fun getActiveAlerts(): Flow<List<SOSAlert>> = sosAlertDao.getActiveAlerts()
    
    // Broadcast SOS signal
    fun broadcastSOS(
        message: String = "EMERGENCY - NEED HELP",
        location: Location? = null,
        mediaBase64: String? = null,
        mediaType: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            try {
                val baseAlert = SOSAlert(
                    senderId = getOwnNodeId(),
                    senderName = "Me",
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    priority = AlertPriority.CRITICAL,
                    mediaBase64 = mediaBase64,
                    mediaType = mediaType,
                    isActive = true
                )

                // Sign the core content
                val dataToSign = "${baseAlert.senderId}|${baseAlert.latitude}|${baseAlert.longitude}|${baseAlert.message}|${baseAlert.timestamp}"
                val signature = identityManager.signData(dataToSign)
                val publicKey = identityManager.getPublicKey()

                val signedAlert = baseAlert.copy(signature = signature, publicKey = publicKey)
                
                // Save to database
                val alertId = sosAlertDao.insertAlert(signedAlert)
                
                // Broadcast via mesh network
                broadcastViaMesh(signedAlert)
                
                onSuccess()
                
            } catch (e: Exception) {
                onError(e.message ?: "Failed to broadcast SOS")
            }
        }
    }
    
    // Receive SOS signal from network
    fun receiveSOS(alert: SOSAlert) {
        scope.launch {
            try {
                // Verify signature before saving
                val dataToVerify = "${alert.senderId}|${alert.latitude}|${alert.longitude}|${alert.message}|${alert.timestamp}"
                if (alert.signature != null && alert.publicKey != null) {
                    val isValid = identityManager.verifySignature(dataToVerify, alert.signature, alert.publicKey)
                    if (!isValid) {
                        android.util.Log.w("SOSService", "Received INVALID signed SOS from ${alert.senderId}. Dropping.")
                        return@launch
                    }
                }

                // Save received alert to database
                sosAlertDao.insertAlert(alert)
                
                // Alert Hardener (System Alerts)
                val alertHardener = net.lifenet.core.utils.AlertHardener(context)
                alertHardener.showNotification("KRİTİK SOS", "${alert.senderId} yardım bekliyor: ${alert.message}", true)
                alertHardener.shutdown()
                
            } catch (e: Exception) {
                android.util.Log.e("SOSService", "Error receiving SOS", e)
            }
        }
    }
    
    // Deactivate SOS alert
    fun deactivateSOS(alertId: Long) {
        scope.launch {
            sosAlertDao.deactivateAlert(alertId)
        }
    }
    
    // Clean up old alerts (older than 24 hours)
    fun cleanupOldAlerts() {
        scope.launch {
            val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            sosAlertDao.deleteOldAlerts(cutoffTime)
        }
    }
    
    private fun broadcastViaMesh(alert: SOSAlert) {
        // SOS|senderId|lat|lon|message|mediaType|mediaBase64|signature|publicKey
        val payload = "SOS|${alert.senderId}|${alert.latitude}|${alert.longitude}|${alert.message}|${alert.mediaType ?: "none"}|${alert.mediaBase64 ?: "none"}|${alert.signature ?: "none"}|${alert.publicKey ?: "none"}".toByteArray()
        // ghostRadioService?.sendMessage(payload)
    }
    
    private fun getOwnNodeId(): String {
        return identityManager.getCurrentNodeId()
    }
}
