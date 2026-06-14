package com.baxter.schedulaizer.data.db.dao

import androidx.room.*
import com.baxter.schedulaizer.data.db.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startMs ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE startMs >= :fromMs AND startMs <= :toMs ORDER BY startMs ASC")
    fun getEventsInRange(fromMs: Long, toMs: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE startMs >= :fromMs ORDER BY startMs ASC")
    fun getUpcomingEvents(fromMs: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE priority <= :maxPriority ORDER BY priority ASC, startMs ASC")
    fun getEventsByPriority(maxPriority: Int): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("SELECT COUNT(*) FROM events WHERE startMs >= :fromMs AND startMs <= :toMs")
    suspend fun countEventsInRange(fromMs: Long, toMs: Long): Int
}
