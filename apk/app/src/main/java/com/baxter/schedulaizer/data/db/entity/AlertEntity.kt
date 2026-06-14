package com.baxter.schedulaizer.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    val parentId: Long,
    /** "event", "bill", or "alarm" (a standalone, user-created alarm). */
    val parentType: String,
    val title: String,
    val body: String,
    val fireAtMs: Long,
    val isActive: Boolean = true,
    val isSnoozed: Boolean = false,
    val snoozeUntilMs: Long = -1L,
    val alarmManagerRequestCode: Int = 0,
    val createdMs: Long = 0L,
    /**
     * Optional per-alert tone (a content:// or file:// Uri string). When null the
     * global tone from settings is used, falling back to the system alarm sound.
     */
    val soundUri: String? = null,
    /** Standalone alarms only: re-arm 24h later after firing instead of deactivating. */
    val repeatDaily: Boolean = false
)
