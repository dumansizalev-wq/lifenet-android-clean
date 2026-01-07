package net.lifenet.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sos_alerts")
data class SOSAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderId: String,
    val senderName: String = "Unknown",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val message: String = "EMERGENCY - NEED HELP",
    val timestamp: Long = System.currentTimeMillis(),
    val priority: AlertPriority = AlertPriority.CRITICAL,
    val mediaBase64: String? = null,
    val mediaType: String? = null, // "image" or "audio"
    val signature: String? = null,
    val publicKey: String? = null,
    val isActive: Boolean = true
)

enum class AlertPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
