package net.lifenet.core.enterprise.audit

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.lifenet.core.enterprise.AuditLogLevel
import net.lifenet.core.enterprise.EnterpriseConfigManager
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Merkezi Audit Logger
 * Enterprise uyumluluğu için tüm kritik işlemleri kaydeder.
 */
class AuditLogger(
    private val context: Context,
    private val configManager: EnterpriseConfigManager
) {
    private val logQueue = ConcurrentLinkedQueue<AuditLogEntry>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val logFile: File by lazy {
        File(context.filesDir, "audit_log.csv")
    }
    
    init {
        // Log dosyasını başlat csv header ile
        if (!logFile.exists()) {
            writeFileHeader()
        }
    }

    fun log(level: AuditLogLevel, eventType: String, details: String, userId: String = "system") {
        // Config'e göre log seviyesini kontrol et
        val config = configManager.config.value
        val minLevel = config?.auditLogLevel ?: AuditLogLevel.INFO
        
        if (level.ordinal < minLevel.ordinal) return

        val entry = AuditLogEntry(
            level = level,
            eventType = eventType,
            userId = userId,
            details = details,
            deviceId = getDeviceId()
        )
        
        logQueue.add(entry)
        
        // Asenkron olarak dosyaya/sunucuya yaz
        scope.launch {
            flushLogs()
        }
    }
    
    private fun flushLogs() {
        if (logQueue.isEmpty()) return
        
        try {
            FileWriter(logFile, true).use { writer ->
                while (!logQueue.isEmpty()) {
                    val entry = logQueue.poll() ?: break
                    val line = formatEntryToCsv(entry)
                    writer.append(line).append("\n")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write audit logs", e)
        }
    }
    
    private fun formatEntryToCsv(entry: AuditLogEntry): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val dateStr = dateFormat.format(Date(entry.timestamp))
        // CSV Injection koruması için tırnak içine alıyoruz
        return "\"$dateStr\",\"${entry.level}\",\"${entry.eventType}\",\"${entry.userId}\",\"${entry.deviceId}\",\"${escapeCsv(entry.details)}\""
    }
    
    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
    
    private fun writeFileHeader() {
        try {
            FileWriter(logFile).use { writer ->
                writer.append("Timestamp,Level,EventType,UserId,DeviceId,Details\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create audit log header", e)
        }
    }
    
    private fun getDeviceId(): String {
        // In real app, get from secure ID provider
        return android.provider.Settings.Secure.getString(
            context.contentResolver, 
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    companion object {
        private const val TAG = "AuditLogger"
    }
}
