package net.lifenet.core.enterprise

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Enterprise yapılandırma modeli
 * MDM sunucusundan alınan politikalar
 */
@Parcelize
data class EnterpriseConfig(
    // Network izolasyonu
    val allowedPeers: Set<String> = emptySet(),
    val allowedPeerDomains: Set<String> = emptySet(),
    
    // KMS yapılandırması
    val kmsEndpoint: String = "",
    val kmsApiKey: String = "",
    val kmsCertificatePinning: List<String> = emptyList(),
    val keyRotationIntervalHours: Int = 24,
    
    // Audit logging
    val auditLogEndpoint: String = "",
    val auditLogLevel: AuditLogLevel = AuditLogLevel.INFO,
    val auditLogBatchSize: Int = 100,
    
    // Güvenlik politikaları
    val maxMessageSize: Int = 1024 * 1024, // 1MB
    val enableRemoteWipe: Boolean = true,
    val enableLocationTracking: Boolean = false,
    val requireDeviceEncryption: Boolean = true,
    val requireScreenLock: Boolean = true,
    val minPasswordLength: Int = 8,
    
    // Özellik bayrakları
    val enableSOS: Boolean = true,
    val enableHumanitarianBridge: Boolean = false,
    val enableOfflineMode: Boolean = true,
    
    // Compliance
    val complianceCheckIntervalMinutes: Int = 60,
    val enforceCompliance: Boolean = true,
    
    // Metadata
    val configVersion: Int = 1,
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable

enum class AuditLogLevel {
    DEBUG, INFO, WARNING, ERROR, CRITICAL
}
