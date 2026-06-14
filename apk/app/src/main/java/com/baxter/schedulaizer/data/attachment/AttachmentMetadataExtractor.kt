package com.baxter.schedulaizer.data.attachment

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns

object AttachmentMetadataExtractor {

    data class Metadata(
        val mimeType: String?,
        val fileName: String?,
        val fileSize: Long?,
        val extension: String?,
        val localPath: String?
    )

    fun extract(context: Context, uri: Uri): Metadata {
        val resolver = context.contentResolver

        val mime = resolver.getType(uri)
        val fileName = queryFileName(resolver, uri)
        val size = queryFileSize(resolver, uri)
        val extension = if (fileName != null && fileName.contains('.')) fileName.substringAfterLast('.') else null
        val localPath = resolveLocalPath(uri)

        return Metadata(
            mimeType = mime,
            fileName = fileName,
            fileSize = size,
            extension = extension,
            localPath = localPath
        )
    }

    private fun queryFileName(resolver: ContentResolver, uri: Uri): String? {
        val cursor: Cursor = resolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            return if (idx >= 0 && it.moveToFirst()) it.getString(idx) else null
        }
    }

    private fun queryFileSize(resolver: ContentResolver, uri: Uri): Long? {
        val cursor: Cursor = resolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val idx = it.getColumnIndex(OpenableColumns.SIZE)
            return if (idx >= 0 && it.moveToFirst()) it.getLong(idx) else null
        }
    }

    private fun resolveLocalPath(uri: Uri): String? {
        return if (uri.scheme == "file") uri.path else null
    }
}
