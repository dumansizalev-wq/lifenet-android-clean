package net.lifenet.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SOSAlertDao {
    
    @Query("SELECT * FROM sos_alerts WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getActiveAlerts(): Flow<List<SOSAlert>>
    
    @Query("SELECT * FROM sos_alerts ORDER BY timestamp DESC LIMIT 50")
    fun getRecentAlerts(): Flow<List<SOSAlert>>
    
    @Insert
    suspend fun insertAlert(alert: SOSAlert): Long
    
    @Query("UPDATE sos_alerts SET isActive = 0 WHERE id = :alertId")
    suspend fun deactivateAlert(alertId: Long)
    
    @Query("DELETE FROM sos_alerts WHERE timestamp < :cutoffTime")
    suspend fun deleteOldAlerts(cutoffTime: Long)
}
