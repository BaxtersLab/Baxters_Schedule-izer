package com.baxter.schedulaizer.transfer

import com.baxter.schedulaizer.data.db.entity.AttachmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL

object BudgetBlasterBridge {
    suspend fun sendReceipt(attachment: AttachmentEntity, endpoint: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val imageBytes = java.util.Base64.getEncoder().encodeToString(java.io.File(attachment.localPath).readBytes())
                val payload = JSONObject().apply {
                    put("date", attachment.capturedMs)
                    put("amount_hint", JSONObject.NULL)
                    put("category", JSONObject.NULL)
                    put("filename", attachment.fileName)
                    put("image_base64", imageBytes)
                }
                val out = BufferedOutputStream(conn.outputStream)
                out.write(payload.toString().toByteArray(Charsets.UTF_8))
                out.flush()
                out.close()
                val code = conn.responseCode
                conn.disconnect()
                code in 200..299
            } catch (e: Exception) {
                false
            }
        }
    }
}
