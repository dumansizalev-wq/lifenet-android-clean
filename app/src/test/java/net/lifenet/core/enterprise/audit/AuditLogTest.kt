package net.lifenet.core.enterprise.audit

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import net.lifenet.core.enterprise.AuditLogLevel
import net.lifenet.core.enterprise.EnterpriseConfig
import net.lifenet.core.enterprise.EnterpriseConfigManager
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File

class AuditLogTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var configManager: EnterpriseConfigManager
    
    private lateinit var auditLogger: AuditLogger
    private lateinit var tempFile: File
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock temporary file for logging
        tempFile = File.createTempFile("audit_test", ".csv")
        `when`(context.filesDir).thenReturn(tempFile.parentFile)
        
        // Mock Config
        val configFlow = MutableStateFlow(EnterpriseConfig(auditLogLevel = AuditLogLevel.INFO))
        `when`(configManager.config).thenReturn(configFlow)
        
        // We override lazy file property strictly speaking needs real file on context in impl
        // But since we use context.filesDir inside, mocking context.filesDir is good enough.
        // NOTE: AuditLogger creates "audit_log.csv" inside filesDir.
        // We probably should clear it or ensure unique name if parallel. 
        // For unit test temp dir is safe.
        
        auditLogger = AuditLogger(context, configManager)
    }
    
    @Test
    fun testLogWriting() {
        // ACT
        auditLogger.log(AuditLogLevel.INFO, "TEST_EVENT", "Test Details")
        
        // Allow async flush
        Thread.sleep(100)
        
        // ASSERT
        // Check if file is written
        val logFile = File(tempFile.parentFile, "audit_log.csv")
        assertTrue(logFile.exists())
        val content = logFile.readText()
        assertTrue(content.contains("TEST_EVENT"))
        assertTrue(content.contains("Test Details"))
    }
    
    @Test
    fun testLevelFiltering() {
        // Config is INFO (Ordinal 1). DEBUG (Ordinal 0) should be skipped.
        
        // ACT
        auditLogger.log(AuditLogLevel.DEBUG, "DEBUG_EVENT", "Should be ignored")
        
        Thread.sleep(50)
        
        // ASSERT
        val logFile = File(tempFile.parentFile, "audit_log.csv")
        val content = logFile.readText()
        assertTrue(!content.contains("DEBUG_EVENT"))
    }
}
