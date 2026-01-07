package net.lifenet.core

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Build flavor yapılandırmasını test eder
 */
@RunWith(JUnit4::class)
class BuildFlavorTest {
    
    @Test
    fun testBuildConfigFields() {
        // BuildConfig alanlarının doğru set edildiğini kontrol et
        assertNotNull(BuildConfig.IS_ENTERPRISE)
        assertNotNull(BuildConfig.ENABLE_MDM)
        assertNotNull(BuildConfig.NETWORK_TYPE)
        
        println("Build Flavor: ${if (BuildConfig.IS_ENTERPRISE) "Enterprise" else "B2C"}")
        println("Network Type: ${BuildConfig.NETWORK_TYPE}")
        println("MDM Enabled: ${BuildConfig.ENABLE_MDM}")
    }
    
    @Test
    fun testFlavorSpecificBehavior() {
        if (BuildConfig.IS_ENTERPRISE) {
            // Enterprise flavor kontrolleri
            assertTrue("Enterprise should have MDM enabled", BuildConfig.ENABLE_MDM)
            // assertTrue("Enterprise should have Remote KMS", BuildConfig.ENABLE_REMOTE_KMS) // Not yet implemented
            
            assertEquals("PRIVATE_MESH", BuildConfig.NETWORK_TYPE)
        } else {
            // B2C flavor kontrolleri
            assertFalse("B2C should NOT have MDM", BuildConfig.ENABLE_MDM)
            assertEquals("PUBLIC_MESH", BuildConfig.NETWORK_TYPE)
        }
    }
}
