package net.lifenet.core.contact

import net.lifenet.core.data.LifenetContact

object ContactManager {
    private val contacts = mutableListOf<LifenetContact>()

    fun addContact(contact: LifenetContact) {
        // Prevent duplicate by Device ID
        if (contacts.none { it.deviceId == contact.deviceId }) {
            contacts.add(contact)
        }
    }

    fun removeContact(deviceId: String) {
        contacts.removeAll { it.deviceId == deviceId }
    }

    fun getAllContacts(): List<LifenetContact> = contacts.toList()

    fun findContact(deviceId: String): LifenetContact? =
        contacts.find { it.deviceId == deviceId }
        
    fun addContactFromQR(qrData: String): Boolean {
        // Expected Format: "LIFENET:<HEX_ID>:<NAME>"
        // Example: "LIFENET:4a3b2c1d9e8f:Operator1"
        return try {
            if (qrData.startsWith("LIFENET:")) {
                val parts = qrData.split(":")
                if (parts.size >= 3) {
                    val id = parts[1]
                    val name = parts[2]
                    if (id.length == 12 || id.length == 24) { // 12 bytes as 24 chars, or just check non-empty
                        addContact(LifenetContact(id, name))
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
