package com.baxter.schedulaizer.data.repository

import com.baxter.schedulaizer.data.db.dao.AttachmentDao
import com.baxter.schedulaizer.data.db.entity.AttachmentEntity
import com.baxter.schedulaizer.util.AttachmentType
import com.baxter.schedulaizer.util.DateUtils
import com.baxter.schedulaizer.util.FileUtils
import kotlinx.coroutines.flow.Flow
import java.io.File

class AttachmentRepository(
    private val attachmentDao: AttachmentDao
) {
    val allAttachments: Flow<List<AttachmentEntity>> = attachmentDao.getAllAttachments()
    val pendingUploads: Flow<List<AttachmentEntity>> = attachmentDao.getPendingUploads()
    val pendingBudgetBlasterSends: Flow<List<AttachmentEntity>> = attachmentDao.getPendingBudgetBlasterSends()
    val activeTransferCount: Flow<Int> = attachmentDao.getActiveTransferCount()
    val activeAttachmentIds: Flow<List<Long>> = attachmentDao.getActiveAttachmentIds()

    fun getForParent(parentId: Long, parentType: String): Flow<List<AttachmentEntity>> = attachmentDao.getAttachmentsForParent(parentId, parentType)

    suspend fun save(attachment: AttachmentEntity): Long {
        if (attachment.localPath.isBlank()) throw IllegalArgumentException("localPath cannot be blank")
        val file = File(attachment.localPath)
        val mime = if (attachment.mimeType.isBlank()) FileUtils.getMimeType(file) else attachment.mimeType
        val attType = if (attachment.attachmentType.isBlank()) AttachmentType.fromMimeType(mime).name else attachment.attachmentType
        val size = if (attachment.fileSizeBytes == 0L && file.exists()) file.length() else attachment.fileSizeBytes
        val now = DateUtils.nowMs()
        val toInsert = attachment.copy(mimeType = mime, attachmentType = attType, fileSizeBytes = size, createdMs = now)
        return attachmentDao.insertAttachment(toInsert)
    }

    // Simple insert wrapper to match newer API expectations
    suspend fun insert(entity: AttachmentEntity): Long {
        return attachmentDao.insertAttachment(entity)
    }

    suspend fun markUploaded(id: Long, destination: String) {
        attachmentDao.markUploaded(id, DateUtils.nowMs(), destination)
    }

    suspend fun markStaged(id: Long) {
        attachmentDao.updateState(id, com.baxter.schedulaizer.data.db.entity.TransferState.STAGED, null, DateUtils.nowMs())
    }

    suspend fun markUploading(id: Long) {
        attachmentDao.updateState(id, com.baxter.schedulaizer.data.db.entity.TransferState.UPLOADING, null, DateUtils.nowMs())
    }

    suspend fun markSuccess(id: Long) {
        attachmentDao.updateState(id, com.baxter.schedulaizer.data.db.entity.TransferState.SUCCESS, null, DateUtils.nowMs())
    }

    suspend fun markFailed(id: Long, reason: String) {
        attachmentDao.updateState(id, com.baxter.schedulaizer.data.db.entity.TransferState.FAILED, reason, DateUtils.nowMs())
    }

    suspend fun get(id: Long): AttachmentEntity? = attachmentDao.getAttachmentById(id)

    suspend fun markSentToBudgetBlaster(id: Long) {
        attachmentDao.markSentToBudgetBlaster(id)
    }

    suspend fun delete(attachment: AttachmentEntity) {
        attachmentDao.deleteAttachment(attachment)
        try {
            FileUtils.deleteFile(File(attachment.localPath))
        } catch (_: Exception) {
            // ignore
        }
    }

    suspend fun getById(id: Long): AttachmentEntity? = attachmentDao.getAttachmentById(id)
}
