package net.lifenet.core.lifecycle

import net.lifenet.core.mode.LifenetMode
import net.lifenet.core.mode.ModeManager
import net.lifenet.core.mode.ModeChangeListener
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * ModeManager için unit testler.
 * Mod geçişleri, callback bildirimleri ve ağ durumu izleme testleri.
 */
class ModeManagerTest {
    
    private lateinit var modeManager: ModeManager
    private var modeChangeCallbackCount = 0
    private var lastOldMode: LifenetMode? = null
    private var lastNewMode: LifenetMode? = null
    
    @Before
    fun setup() {
        modeManager = ModeManager.getInstance()
        // Reset state if singleton (which it is now)
        modeManager.switchToDailyMode()
        modeChangeCallbackCount = 0
        lastOldMode = null
        lastNewMode = null
    }
    
    @Test
    fun testInitialModeIsDaily() {
        assertEquals(LifenetMode.DAILY, modeManager.currentMode)
        assertTrue(modeManager.isDailyMode())
        assertFalse(modeManager.isDisasterMode())
    }
    
    @Test
    fun testManualSwitchToDisasterMode() {
        modeManager.switchToDisasterMode()
        
        assertEquals(LifenetMode.DISASTER, modeManager.currentMode)
        assertTrue(modeManager.isDisasterMode())
        assertFalse(modeManager.isDailyMode())
    }
    
    @Test
    fun testManualSwitchToDailyMode() {
        // Önce DISASTER moduna geç
        modeManager.switchToDisasterMode()
        
        // Sonra DAILY moduna geri dön
        modeManager.switchToDailyMode()
        
        assertEquals(LifenetMode.DAILY, modeManager.currentMode)
        assertTrue(modeManager.isDailyMode())
    }
    
    @Test
    fun testModeChangeListenerCallback() {
        val listener = object : ModeChangeListener {
            override fun onModeChanged(newMode: LifenetMode, oldMode: LifenetMode) {
                modeChangeCallbackCount++
                lastOldMode = oldMode
                lastNewMode = newMode
            }
        }
        
        modeManager.addModeChangeListener(listener)
        modeManager.switchToDisasterMode()
        
        assertEquals(1, modeChangeCallbackCount)
        assertEquals(LifenetMode.DAILY, lastOldMode)
        assertEquals(LifenetMode.DISASTER, lastNewMode)
        
        modeManager.removeModeChangeListener(listener)
    }
    
    @Test
    fun testMultipleListeners() {
        var listener1Called = false
        var listener2Called = false
        
        val listener1 = object : ModeChangeListener {
            override fun onModeChanged(newMode: LifenetMode, oldMode: LifenetMode) {
                listener1Called = true
            }
        }
        
        val listener2 = object : ModeChangeListener {
            override fun onModeChanged(newMode: LifenetMode, oldMode: LifenetMode) {
                listener2Called = true
            }
        }
        
        modeManager.addModeChangeListener(listener1)
        modeManager.addModeChangeListener(listener2)
        modeManager.switchToDisasterMode()
        
        assertTrue(listener1Called)
        assertTrue(listener2Called)
        
        modeManager.removeModeChangeListener(listener1)
        modeManager.removeModeChangeListener(listener2)
    }
    
    @Test
    fun testRemoveListener() {
        // Reset count
        modeChangeCallbackCount = 0
        
        val listener = object : ModeChangeListener {
            override fun onModeChanged(newMode: LifenetMode, oldMode: LifenetMode) {
                modeChangeCallbackCount++
            }
        }
        
        modeManager.addModeChangeListener(listener)
        modeManager.switchToDisasterMode()
        assertEquals(1, modeChangeCallbackCount)
        
        // Listener'ı kaldır
        modeManager.removeModeChangeListener(listener)
        modeManager.switchToDailyMode()
        
        // Callback sayısı artmamalı
        assertEquals(1, modeChangeCallbackCount)
    }
    
    @Test
    fun testNetworkAvailableKeepsDailyMode() {
        modeManager.switchToDailyMode()
        modeManager.checkNetworkAndSwitch(isNetworkAvailable = true)
        assertEquals(LifenetMode.DAILY, modeManager.currentMode)
    }
    
    @Test
    fun testNetworkUnavailableForShortTimeKeepsDailyMode() {
        modeManager.switchToDailyMode()
        // Ağ yok ama 5 dakika geçmemiş
        modeManager.checkNetworkAndSwitch(isNetworkAvailable = false)
        
        // Hala DAILY modda olmalı (timeout henüz geçmedi)
        assertEquals(LifenetMode.DAILY, modeManager.currentMode)
    }
    
    @Test
    fun testIdempotentModeSwitching() {
        modeChangeCallbackCount = 0
        // Aynı moda birden fazla geçiş callback tetiklememeli
        val listener = object : ModeChangeListener {
            override fun onModeChanged(newMode: LifenetMode, oldMode: LifenetMode) {
                modeChangeCallbackCount++
            }
        }
        
        modeManager.addModeChangeListener(listener)
        
        modeManager.switchToDisasterMode()
        assertEquals(1, modeChangeCallbackCount)
        
        // Tekrar DISASTER moduna geçmeye çalış
        modeManager.switchToDisasterMode()
        
        // Callback sayısı artmamalı
        assertEquals(1, modeChangeCallbackCount)
        
        modeManager.removeModeChangeListener(listener)
    }
}
