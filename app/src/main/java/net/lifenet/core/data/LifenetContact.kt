package net.lifenet.core.data

data class LifenetContact(
    val deviceId: String,      // Cihaz Hash/ID
    val displayName: String,   // Takma ad
    val lastSeen: Long = System.currentTimeMillis()
)
