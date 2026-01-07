package net.lifenet.core.enterprise.audit

import net.lifenet.core.enterprise.AuditLogLevel

data class AuditLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: AuditLogLevel,
    val eventType: String,
    val userId: String,
    val details: String,
    val deviceId: String
)
