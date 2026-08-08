package com.byebyechallan.app.data.model

// What the app sends when saving/updating a document record.
data class DocumentRequestDto(
    val id: Long = 0L,
    val docTemplateId: String,
    val docName: String,
    val docId: String,
    val expiryDate: String?,
    val notificationTime: String?,
    val fileName: String? = null,
    val email: Boolean = false,
    val whatsApp: Boolean = false,
    val sms: Boolean = false,
    val uploaded: Boolean = false
)

// What the backend returns for a saved/existing document
data class UserDocumentDto(
    val id: Long,
    val userId: Long,
    val profileId: Long,
    val vehicleRegistrationNo: String,
    val docTemplateId: String?,
    val docId: String?,
    val docName: String?,
    val s3Link: String?,
    val fileName: String? = null,
    val uploaded: Boolean = false,
    val uploadedDate: String?,
    val expiryDate: String?,
    val notificationTime: String?,
    val createDate: String?,
    val updatedDate: String?,
    val email: Boolean = false,
    val whatsApp: Boolean = false,
    val sms: Boolean = false
) {
    fun hasValidFile(): Boolean {
        val hasFileName = !fileName.isNullOrBlank() && fileName != "No File Name"
        val hasLegacyLink = !s3Link.isNullOrBlank() && s3Link != "No Link Available"
        return uploaded && (hasFileName || hasLegacyLink)
    }

    fun displayFileName(): String? {
        return fileName?.takeUnless { it.isBlank() || it == "No File Name" }
            ?: s3Link?.substringAfterLast('/')?.takeUnless { it.isBlank() || it == "No Link Available" }
    }

    fun resolvePreviewUrl(userId: Long, baseUrl: String): String? {
        val serverFileKey = s3Link?.substringAfterLast('/')
            ?.takeUnless { it.isBlank() || it == "No Link Available" }
        if (serverFileKey != null) {
            return com.byebyechallan.app.util.FileUrlBuilder.downloadUrl(baseUrl, userId, serverFileKey)
        }
        return s3Link?.takeUnless { it.isBlank() || it == "No Link Available" }
    }
}


data class FileResponseDto(
    val fileName: String? = null,
    val originalFileName: String? = null,
    val filePath: String? = null,
    val fileUrl: String? = null
) {
    /** Server-side stored name used for download API path. */
    val storedFileName: String?
        get() = fileName?.takeIf { it.isNotBlank() }
            ?: filePath?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: fileUrl?.substringAfterLast('/')?.takeIf { it.isNotBlank() }

    /** Human-readable name shown in the UI and saved on the document record. */
    val displayFileName: String?
        get() = originalFileName?.takeIf { it.isNotBlank() } ?: storedFileName
}

// A merged view used on the Android side: combines a checklist item (what the backend returns)
// with the matching uploaded record (UserDocumentDto) if one exists, so the Vehicle Detail screen
// can show "uploaded" vs "pending" in one list.
data class DocumentChecklistItem(
    val template: DocumentRequestDto,
    val uploaded: UserDocumentDto?
) {
    val isUploaded: Boolean get() = uploaded?.hasValidFile() == true
    // Backend no longer returns a docType field in the checklist/template objects per OpenAPI.
    // isMandatory defaults to false; UI should be updated when the backend adds a field to indicate mandatory.
    val isMandatory: Boolean get() = false
}
