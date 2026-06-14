package com.baxter.schedulaizer.data.db.dao

import androidx.room.*
import com.baxter.schedulaizer.data.db.entity.AttachmentEntity
import com.baxter.schedulaizer.data.db.entity.TransferState
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments ORDER BY capturedMs DESC")
    fun getAllAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE parentId = :parentId AND parentType = :parentType ORDER BY capturedMs DESC")
    fun getAttachmentsForParent(parentId: Long, parentType: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE uploadedMs = -1 ORDER BY capturedMs ASC")
    fun getPendingUploads(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE isSentToBudgetBlaster = 0 AND mimeType LIKE 'image/%' ORDER BY capturedMs DESC")
    fun getPendingBudgetBlasterSends(): Flow<List<AttachmentEntity>>

    @Query("SELECT COUNT(*) FROM attachments WHERE state IN ('STAGED','UPLOADING')")
    fun getActiveTransferCount(): Flow<Int>

    @Query("SELECT id FROM attachments WHERE state IN ('STAGED','UPLOADING')")
    fun getActiveAttachmentIds(): Flow<List<Long>>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getAttachmentById(id: Long): AttachmentEntity?

    @Query("UPDATE attachments SET state = :state, errorMessage = :error, updatedAt = :ts WHERE id = :id")
    suspend fun updateState(id: Long, state: TransferState, error: String?, ts: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity): Long

    @Update
    suspend fun updateAttachment(attachment: AttachmentEntity)

    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    @Query("UPDATE attachments SET uploadedMs = :uploadedMs, uploadDestination = :destination WHERE id = :id")
    suspend fun markUploaded(id: Long, uploadedMs: Long, destination: String)

    @Query("UPDATE attachments SET isSentToBudgetBlaster = 1 WHERE id = :id")
    suspend fun markSentToBudgetBlaster(id: Long)
}
