package com.baxter.schedulaizer.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    val name: String,
    val amountCents: Long = 0L,
    val dueDayOfMonth: Int,
    val intervalMonths: Int = 1,
    val nextDueMs: Long = 0L,
    val isPaid: Boolean = false,
    val priority: Int = 2,
    val category: String = "BILLS",
    val notes: String = "",
    val alertOffsetMinutes: Int = 1440,
    val createdMs: Long = 0L,
    val updatedMs: Long = 0L
)
