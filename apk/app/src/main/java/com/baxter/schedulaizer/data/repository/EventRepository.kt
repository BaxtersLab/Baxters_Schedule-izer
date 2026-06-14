package com.baxter.schedulaizer.data.repository

import com.baxter.schedulaizer.data.db.dao.EventDao
import com.baxter.schedulaizer.data.db.dao.AlertDao
import com.baxter.schedulaizer.data.db.entity.EventEntity
import com.baxter.schedulaizer.util.DateUtils
import kotlinx.coroutines.flow.Flow

class EventRepository(
    private val eventDao: EventDao,
    private val alertDao: AlertDao
) {
    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()
    val upcomingEvents: Flow<List<EventEntity>> = eventDao.getUpcomingEvents(DateUtils.todayStartMs())

    fun getEventsInRange(fromMs: Long, toMs: Long): Flow<List<EventEntity>> = eventDao.getEventsInRange(fromMs, toMs)

    suspend fun save(event: EventEntity): Long {
        if (event.title.isBlank()) throw IllegalArgumentException("Event title cannot be blank")
        if (event.endMs < event.startMs) throw IllegalArgumentException("Event end must be after start")
        val now = DateUtils.nowMs()
        return if (event.id == 0L) {
            val toInsert = event.copy(createdMs = now, updatedMs = now)
            eventDao.insertEvent(toInsert)
        } else {
            val toUpdate = event.copy(updatedMs = now)
            eventDao.updateEvent(toUpdate)
            toUpdate.id
        }
    }

    suspend fun delete(event: EventEntity) {
        eventDao.deleteEvent(event)
    }

    suspend fun getById(id: Long): EventEntity? = eventDao.getEventById(id)

    fun getByPriority(maxPriority: Int): Flow<List<EventEntity>> = eventDao.getEventsByPriority(maxPriority)

    suspend fun countInRange(fromMs: Long, toMs: Long): Int = eventDao.countEventsInRange(fromMs, toMs)
}
