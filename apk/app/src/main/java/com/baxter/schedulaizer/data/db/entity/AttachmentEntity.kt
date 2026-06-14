package com.baxter.schedulaizer.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.baxter.schedulaizer.data.db.entity.TransferState

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    val parentId: Long = 0L,
    val parentType: String = "",
    val localPath: String,
    val mimeType: String,
    val attachmentType: String,
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    val capturedMs: Long = 0L,
    val uploadedMs: Long = -1L,
    val uploadDestination: String = "",
    val isSentToBudgetBlaster: Boolean = false,
    val createdMs: Long = 0L
    // Transfer state machine fields
    ,
    val state: TransferState = TransferState.PENDING,
    val errorMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
