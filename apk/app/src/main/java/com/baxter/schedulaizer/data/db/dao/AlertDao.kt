package com.baxter.schedulaizer.data.db.dao

import androidx.room.*
import com.baxter.schedulaizer.data.db.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts WHERE isActive = 1 ORDER BY fireAtMs ASC")
    fun getActiveAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE parentId = :parentId AND parentType = :parentType")
    fun getAlertsForParent(parentId: Long, parentType: String): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun getAlertById(id: Long): AlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Query("UPDATE alerts SET isActive = 0 WHERE id = :id")
    suspend fun deactivateAlert(id: Long)

    @Query("UPDATE alerts SET isSnoozed = 1, snoozeUntilMs = :snoozeUntilMs WHERE id = :id")
    suspend fun snoozeAlert(id: Long, snoozeUntilMs: Long)

    @Query("DELETE FROM alerts WHERE parentId = :parentId AND parentType = :parentType")
    suspend fun deleteAlertsForParent(parentId: Long, parentType: String)

    @Query("SELECT * FROM alerts WHERE isActive = 1 AND fireAtMs <= :nowMs")
    suspend fun getOverdueAlerts(nowMs: Long): List<AlertEntity>

    // --- Standalone alarms (parentType = "alarm") ---

    @Query("SELECT * FROM alerts WHERE parentType = 'alarm' ORDER BY fireAtMs ASC")
    fun getAlarms(): Flow<List<AlertEntity>>

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE alerts SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
}
