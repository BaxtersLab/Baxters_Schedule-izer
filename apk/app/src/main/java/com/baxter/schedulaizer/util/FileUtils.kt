package com.baxter.schedulaizer.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.net.URLConnection
import java.text.DecimalFormat

object FileUtils {
    fun capturesDir(context: Context): File {
        val dir = File(context.filesDir, "captures")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun voiceMemosDir(context: Context): File {
        val dir = File(context.filesDir, "voice_memos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun newReceiptFile(context: Context): File {
        val ts = System.currentTimeMillis()
        return File(capturesDir(context), "receipt_${ts}.jpg")
    }

    fun newVoiceMemoFile(context: Context): File {
        val ts = System.currentTimeMillis()
        return File(voiceMemosDir(context), "memo_${ts}.m4a")
    }

    fun newScreenshotFile(context: Context): File {
        val ts = System.currentTimeMillis()
        return File(capturesDir(context), "screenshot_${ts}.jpg")
    }

    @Throws(IOException::class)
    fun newTextNoteFile(context: Context, content: String): File {
        val ts = System.currentTimeMillis()
        val f = File(capturesDir(context), "note_${ts}.txt")
        f.writeText(content, Charsets.UTF_8)
        return f
    }

    fun getMimeType(file: File): String {
        return URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"
    }

    fun getContentUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.provider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun formatFileSize(bytes: Long): String {
        val df = DecimalFormat("#.#")
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1_048_576 -> "${df.format(bytes / 1024.0)} KB"
            bytes < 1_073_741_824 -> "${df.format(bytes / 1_048_576.0)} MB"
            else -> "${df.format(bytes / 1_073_741_824.0)} GB"
        }
    }

    fun deleteFile(file: File): Boolean = file.exists() && file.delete()
}
