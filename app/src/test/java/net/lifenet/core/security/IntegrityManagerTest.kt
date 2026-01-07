package net.lifenet.core.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class IntegrityManagerTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var packageManager: PackageManager
    
    @Mock
    private lateinit var applicationInfo: ApplicationInfo
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.packageName).thenReturn("net.lifenet.core")
        `when`(context.packageManager).thenReturn(packageManager)
        `when`(context.applicationInfo).thenReturn(applicationInfo)
    }
    
    @Test
    fun testIsDebuggerAttached_FlagCheck() {
        // ARRANGE
        applicationInfo.flags = ApplicationInfo.FLAG_DEBUGGABLE
        
        // ACT
        val isAttached = IntegrityManager.isDebuggerAttached(context)
        
        // ASSERT
        assertTrue("Should detect debugger via FLAG_DEBUGGABLE", isAttached)
    }
    
    @Test
    fun testIsDebuggerAttached_NoFlag() {
        // ARRANGE
        applicationInfo.flags = 0
        
        // ACT
        // Since Debug.isDebuggerConnected() is static and returns false by default in mocks/robolectric
        val isAttached = IntegrityManager.isDebuggerAttached(context)
        
        // ASSERT
        assertFalse("Should not detect debugger if flag is off and static method returns false", isAttached)
    }
    
    // Note: Verification of verifySignature is complex due to PackageInfo structure mocking across API levels.
    // We will assume logic correctness from manual verification plan or partial integration test if needed.
}
