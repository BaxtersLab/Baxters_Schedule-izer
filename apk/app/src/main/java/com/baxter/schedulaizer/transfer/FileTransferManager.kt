package com.baxter.schedulaizer.transfer

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import com.baxter.schedulaizer.transfer.RemoteDexterConfigProvider

object FileTransferManager {

    lateinit var attachmentRepository: com.baxter.schedulaizer.data.repository.AttachmentRepository
    lateinit var appContextProvider: () -> Context

    sealed class Event {
        data class Progress(val percent: Int) : Event()
        object Success : Event()
        data class Error(val message: String) : Event()
        object Staged : Event()
    }

    /**
     * Sends a file identified by [attachmentId] and [uri]. Emits stateful events and
     * updates the attachment repository accordingly.
     */
    fun sendFile(attachmentId: Long, uri: Uri) = callbackFlow<Event> {
        val ctx = appContextProvider()
        val resolver = ctx.contentResolver

        val job = launch {
            try {
                // mark as uploading
                try { attachmentRepository.markUploading(attachmentId) } catch (_: Throwable) {}

                // STAGED MODE
                if (!RemoteDexterNative.libraryLoaded) {
                    try { attachmentRepository.markStaged(attachmentId) } catch (_: Throwable) {}
                    trySend(Event.Staged)
                    return@launch
                }

                val config = RemoteDexterConfigProvider.get()
                val initOk = try { RemoteDexterNative.init() } catch (_: Throwable) { false }
                val connected = if (initOk) {
                    try { RemoteDexterNative.connect(config.host, config.port) } catch (_: Throwable) { false }
                } else false

                // Prefer native bulk send when localPath is available; otherwise stream chunks
                val attachment = try { attachmentRepository.get(attachmentId) } catch (_: Throwable) { null }
                val remotePath = if (attachment != null && attachment.uploadDestination.isNotBlank()) attachment.uploadDestination else "/remote/${uri.lastPathSegment ?: "upload"}"

                val localPath = attachment?.localPath
                if (!localPath.isNullOrBlank()) {
                    val file = File(localPath)

                    if (file.exists() && file.canRead() && file.length() > 0) {
                        val ok = try {
                            withContext(Dispatchers.IO) { RemoteDexterNative.sendFile(localPath, remotePath) }
                        } catch (_: Throwable) { false }

                        if (!ok) {
                            try { attachmentRepository.markFailed(attachmentId, "Native sendFile() failed") } catch (_: Throwable) {}
                            trySend(Event.Error("Native sendFile() failed"))
                            return@launch
                        }

                        try { attachmentRepository.markSuccess(attachmentId) } catch (_: Throwable) {}
                        trySend(Event.Progress(100))
                        trySend(Event.Success)
                        return@launch
                    }
                    // If file does NOT exist or is unreadable → fall through to chunked streaming
                }

                // FALLBACK: streaming mode
                withContext(Dispatchers.IO) {
                    val inputStream: InputStream = resolver.openInputStream(uri)
                        ?: throw IllegalStateException("Unable to open input stream")

                    val pfd = resolver.openFileDescriptor(uri, "r")
                    val totalBytes = pfd?.statSize ?: -1L

                    val bis = if (inputStream is BufferedInputStream) inputStream else BufferedInputStream(inputStream)
                    try {
                        val bufferSize = 64 * 1024
                        val buffer = ByteArray(bufferSize)
                        var read: Int
                        var totalRead = 0L

                        var lastEmitTime = 0L
                        var lastPercent = -1

                        while (true) {
                            read = bis.read(buffer)
                            if (read <= 0) break
                            val chunk = if (read == bufferSize) buffer else buffer.copyOf(read)

                            val ok = try { RemoteDexterNative.sendBytes(chunk, remotePath) } catch (_: Throwable) { false }
                            if (!ok) {
                                try { attachmentRepository.markFailed(attachmentId, "Remote send failed") } catch (_: Throwable) {}
                                trySend(Event.Error("Remote send failed"))
                                return@withContext
                            }

                            totalRead += read
                            if (totalBytes > 0) {
                                val percent = ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                                val now = System.currentTimeMillis()
                                var shouldEmit = false

                                if (percent != lastPercent && now - lastEmitTime >= 250) {
                                    shouldEmit = true
                                }

                                if (shouldEmit) {
                                    trySend(Event.Progress(percent))
                                    lastPercent = percent
                                    lastEmitTime = now
                                }
                            }
                        }

                        // Ensure final progress and success are emitted
                        trySend(Event.Progress(100))
                        try { attachmentRepository.markSuccess(attachmentId) } catch (_: Throwable) {}
                        trySend(Event.Success)
                    } finally {
                        try { bis.close() } catch (_: Exception) {}
                        try { pfd?.close() } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                try { attachmentRepository.markFailed(attachmentId, e.message ?: "Unknown error") } catch (_: Throwable) {}
                trySend(Event.Error(e.message ?: "Unknown error"))
            }
        }

        awaitClose { job.cancel() }
    }

}
