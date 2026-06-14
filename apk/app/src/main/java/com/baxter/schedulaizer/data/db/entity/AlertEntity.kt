package com.baxter.schedulaizer.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    val parentId: Long,
    val parentType: String,
    val title: String,
    val body: String,
    val fireAtMs: Long,
    val isActive: Boolean = true,
    val isSnoozed: Boolean = false,
    val snoozeUntilMs: Long = -1L,
    val alarmManagerRequestCode: Int = 0,
    val createdMs: Long = 0L
)
