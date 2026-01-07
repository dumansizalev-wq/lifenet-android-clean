package net.lifenet.core.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * LifenetPerformanceMonitor: Batarya ve bellek metriklerini izler
 * P0 Requirements:
 * - Idle battery ≤2%/hour
 * - SOS battery ≤15%/hour
 * - Heap usage ≤150MB
 */
class LifenetPerformanceMonitor(private val context: Context) {
    
    private var monitoringJob: Job? = null
    private var isMonitoring = false
    
    // Battery tracking
    private var startBatteryLevel: Int = 0
    private var startTimestamp: Long = 0
    private val batteryReadings = mutableListOf<BatteryReading>()
    
    // Memory tracking
    private val memoryReadings = mutableListOf<MemoryReading>()
    
    /**
     * Batarya izlemeyi başlat
     */
    fun startBatteryMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "Already monitoring")
            return
        }
        
        startBatteryLevel = getCurrentBatteryLevel()
        startTimestamp = System.currentTimeMillis()
        batteryReadings.clear()
        isMonitoring = true
        
        Log.i(TAG, "Battery monitoring started at $startBatteryLevel%")
        
        // Periyodik okuma başlat
        startPeriodicBatteryCheck()
    }
    
    /**
     * Batarya izlemeyi durdur
     */
    fun stopBatteryMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        Log.i(TAG, "Battery monitoring stopped")
    }
    
    /**
     * Saat başına batarya tüketimi (%)
     */
    fun getBatteryDrainPerHour(): Float {
        if (batteryReadings.isEmpty()) return 0f
        
        val currentLevel = getCurrentBatteryLevel()
        val elapsedMs = System.currentTimeMillis() - startTimestamp
        val elapsedHours = elapsedMs / 3600000f
        
        if (elapsedHours < 0.01f) return 0f // Çok kısa süre
        
        val drainPercent = startBatteryLevel - currentLevel
        return drainPercent / elapsedHours
    }
    
    /**
     * SOS aktif iken batarya tüketimi
     */
    fun getSOSBatteryDrain(): Float {
        // SOS aktif olan okumalardan ortalama hesapla
        val sosReadings = batteryReadings.filter { it.sosActive }
        if (sosReadings.size < 2) return 0f
        
        val first = sosReadings.first()
        val last = sosReadings.last()
        val elapsedHours = (last.timestamp - first.timestamp) / 3600000f
        
        if (elapsedHours < 0.01f) return 0f
        
        val drainPercent = first.batteryLevel - last.batteryLevel
        return drainPercent / elapsedHours
    }
    
    /**
     * Mevcut heap kullanımı (MB)
     */
    fun getHeapUsage(): Long {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024 * 1024) // MB
    }
    
    /**
     * Bellek sızıntısı tespiti (basit)
     */
    fun detectMemoryLeaks(): List<MemoryLeak> {
        val leaks = mutableListOf<MemoryLeak>()
        
        // Son 10 okumada sürekli artış var mı?
        if (memoryReadings.size >= 10) {
            val recent = memoryReadings.takeLast(10)
            var increasing = true
            
            for (i in 1 until recent.size) {
                if (recent[i].heapMB <= recent[i-1].heapMB) {
                    increasing = false
                    break
                }
            }
            
            if (increasing) {
                leaks.add(MemoryLeak(
                    component = "Unknown",
                    description = "Continuous memory increase detected",
                    heapGrowthMB = recent.last().heapMB - recent.first().heapMB
                ))
            }
        }
        
        return leaks
    }
    
    /**
     * Batarya tüketimi assertion (test için)
     */
    fun assertBatteryDrain(maxPercentPerHour: Float) {
        val actual = getBatteryDrainPerHour()
        if (actual > maxPercentPerHour) {
            throw AssertionError("Battery drain $actual%/hr exceeds limit $maxPercentPerHour%/hr")
        }
        Log.i(TAG, "✅ Battery drain verified: $actual%/hr ≤ $maxPercentPerHour%/hr")
    }
    
    /**
     * Heap kullanımı assertion (test için)
     */
    fun assertHeapUsage(maxMB: Int) {
        val actual = getHeapUsage()
        if (actual > maxMB) {
            throw AssertionError("Heap usage ${actual}MB exceeds limit ${maxMB}MB")
        }
        Log.i(TAG, "✅ Heap usage verified: ${actual}MB ≤ ${maxMB}MB")
    }
    
    private fun getCurrentBatteryLevel(): Int {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            -1
        }
    }
    
    private fun startPeriodicBatteryCheck() {
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isMonitoring) {
                recordBatteryReading()
                recordMemoryReading()
                delay(60000) // Her 1 dakikada bir
            }
        }
    }
    
    private fun recordBatteryReading() {
        val reading = BatteryReading(
            timestamp = System.currentTimeMillis(),
            batteryLevel = getCurrentBatteryLevel(),
            sosActive = false // TODO: SOS durumunu kontrol et
        )
        batteryReadings.add(reading)
        
        Log.d(TAG, "Battery reading: ${reading.batteryLevel}%")
    }
    
    private fun recordMemoryReading() {
        val reading = MemoryReading(
            timestamp = System.currentTimeMillis(),
            heapMB = getHeapUsage()
        )
        memoryReadings.add(reading)
        
        Log.d(TAG, "Memory reading: ${reading.heapMB}MB")
    }
    
    companion object {
        private const val TAG = "LifenetPerformanceMonitor"
    }
}

data class BatteryReading(
    val timestamp: Long,
    val batteryLevel: Int,
    val sosActive: Boolean
)

data class MemoryReading(
    val timestamp: Long,
    val heapMB: Long
)

data class MemoryLeak(
    val component: String,
    val description: String,
    val heapGrowthMB: Long
)
