package net.lifenet.core.security.audit

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class SecurityAuditTest {

    private val manifestPath = "src/main/AndroidManifest.xml" // Relative to app module root in unit test

    @Test
    fun testManifestHardening() {
        val manifestFile = File(manifestPath)
        // Adjust path if running from project root vs app dir
        val fileToRead = if (manifestFile.exists()) manifestFile else File("app/$manifestPath")
        
        assertTrue("AndroidManifest.xml not found at ${fileToRead.absolutePath}", fileToRead.exists())

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(fileToRead)
        
        doc.documentElement.normalize()
        
        val applicationNode = doc.getElementsByTagName("application").item(0)
        val attributes = applicationNode.attributes
        
        // 1. Check AllowBackup
        val allowBackup = attributes.getNamedItem("android:allowBackup")
        assertNotNull("android:allowBackup must be explicitly set", allowBackup)
        assertEquals("android:allowBackup must be false", "false", allowBackup.nodeValue)
        
        // 2. Check CleartextTraffic
        val cleartextTraffic = attributes.getNamedItem("android:usesCleartextTraffic")
        assertNotNull("android:usesCleartextTraffic must be explicitly set", cleartextTraffic)
        assertEquals("android:usesCleartextTraffic must be false", "false", cleartextTraffic.nodeValue)
    }
    
    @Test
    fun testExportedComponents() {
        val manifestFile = File(manifestPath)
        val fileToRead = if (manifestFile.exists()) manifestFile else File("app/$manifestPath")
        
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(fileToRead)
        
        val services = doc.getElementsByTagName("service")
        for (i in 0 until services.length) {
            val service = services.item(i)
            val exported = service.attributes.getNamedItem("android:exported")
            
            // Services should default to not exported or be explicitly false unless necessary
            if (exported != null && exported.nodeValue == "true") {
                // If we had a public service, we'd list it as exception here
                fail("Service ${service.attributes.getNamedItem("android:name").nodeValue} is exported!")
            }
        }
    }
}
