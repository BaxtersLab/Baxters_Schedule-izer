package com.baxter.schedulaizer.util

enum class AttachmentType(val displayName: String) {
    RECEIPT_IMAGE("Receipt Photo"),
    FIELD_PHOTO("Field Photo"),
    SCREENSHOT("Screenshot"),
    VOICE_MEMO("Voice Memo"),
    TEXT_NOTE("Text Note"),
    DOCUMENT("Document");

    companion object {
        fun fromMimeType(mime: String): AttachmentType {
            val m = mime.lowercase()
            return when {
                m.startsWith("image/") -> RECEIPT_IMAGE
                m.startsWith("audio/") -> VOICE_MEMO
                m == "text/plain" -> TEXT_NOTE
                m == "application/pdf" -> DOCUMENT
                else -> FIELD_PHOTO
            }
        }
    }
}
