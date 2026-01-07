package net.lifenet.core.ui

import net.lifenet.core.data.DeliveryStatus

data class Conversation(
    val contactId: String,
    val contactName: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val isSentByMe: Boolean,
    val deliveryStatus: DeliveryStatus?
)
