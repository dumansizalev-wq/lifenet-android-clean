package net.lifenet.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey
    val id: String,
    val name: String,
    val phoneNumber: String? = null,
    val publicKey: String? = null,
    val lastSeen: Long = 0,
    val isEmergencyContact: Boolean = false,
    val isSyncedFromPhone: Boolean = false
)
