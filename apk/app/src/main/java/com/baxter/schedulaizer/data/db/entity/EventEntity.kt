package com.baxter.schedulaizer.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    val title: String,
    val description: String = "",
    val startMs: Long,
    val endMs: Long,
    val isAllDay: Boolean = false,
    val category: String = "PERSONAL",
    val priority: Int = 2,
    val recurrenceRule: String = "",
    val alertOffsetMinutes: Int = 15,
    val createdMs: Long = 0L,
    val updatedMs: Long = 0L
)
