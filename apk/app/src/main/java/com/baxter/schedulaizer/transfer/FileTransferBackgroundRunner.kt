package com.baxter.schedulaizer.transfer

import com.baxter.schedulaizer.data.db.entity.AttachmentEntity
import com.baxter.schedulaizer.data.repository.AttachmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileTransferBackgroundRunner {

    suspend fun run(
        attachmentId: Long,
        repo: AttachmentRepository,
        progressCallback: suspend (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val attachment = repo.get(attachmentId)
            ?: throw IllegalStateException("Attachment not found: $attachmentId")

        repo.markUploading(attachmentId)

        if (!RemoteDexterNative.libraryLoaded) {
            repo.markStaged(attachmentId)
            return@withContext
        }

        val remotePath = if (attachment.uploadDestination.isNotBlank()) attachment.uploadDestination else "/remote/${attachment.fileName}"
        val localPath = attachment.localPath

        if (!localPath.isNullOrBlank()) {
            val file = File(localPath)
            if (file.exists() && file.canRead() && file.length() > 0) {
                val ok = try { RemoteDexterNative.sendFile(localPath, remotePath) } catch (_: Throwable) { false }
                if (!ok) {
                    repo.markFailed(attachmentId, "Native sendFile() failed")
                    return@withContext
                }
                // bulk send completed
                progressCallback(100)
                repo.markSuccess(attachmentId)
                return@withContext
            }
        }

        // Fallback: stream local file in chunks (if present)
        if (!localPath.isNullOrBlank()) {
            val f = File(localPath)
            if (f.exists() && f.canRead()) {
                val totalBytes = f.length()
                var totalSent = 0L
                f.inputStream().buffered().use { stream ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read <= 0) break
                        val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                        val ok = try { RemoteDexterNative.sendBytes(chunk, remotePath) } catch (_: Throwable) { false }
                        if (!ok) {
                            repo.markFailed(attachmentId, "Remote send failed")
                            return@withContext
                        }
                        totalSent += read
                        if (totalBytes > 0) {
                            val percent = ((totalSent * 100) / totalBytes).toInt().coerceIn(0, 100)
                            progressCallback(percent)
                        }
                    }
                }
                progressCallback(100)
                repo.markSuccess(attachmentId)
                return@withContext
            }
        }

        // Fallback: no local file available -> mark staged
        repo.markStaged(attachmentId)
    }
}
