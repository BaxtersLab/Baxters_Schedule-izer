package com.baxter.schedulaizer.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    val parentId: Long = 0L,
    val parentType: String = "",
    val content: String = "",
    val title: String = "",
    val priority: Int = 2,
    val createdMs: Long = 0L,
    val updatedMs: Long = 0L
)
