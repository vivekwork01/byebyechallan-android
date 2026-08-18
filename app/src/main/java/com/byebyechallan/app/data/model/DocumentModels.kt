package com.byebyechallan.app.data.model

import com.byebyechallan.app.util.FileUrlBuilder

data class RCDto(
    val registrationNo: String? = null,
    val registrationDate: String? = null,
    val expiryDate: String? = null,
    val rcS3Link: String? = null
)

data class DocumentDto(
    val documentRequestDto: DocumentRequestDto,
    val rcDto: RCDto? = null
)

// What the app sends when saving/updating a document record.
data class DocumentRequestDto(
    val id: Long = 0L,
    val docTemplateId: String,
    val docName: String,
    val docId: String,
    val expiryDate: String?,
    val fileName: String? = null,
    val s3FileName: String? = null,
    val email: Boolean = false,
    val whatsApp: Boolean = false,
    val sms: Boolean = false,
    val uploaded: Boolean = false,
    val renewable: Boolean = false
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
    val s3FileName: String? = null,
    val uploaded: Boolean = false,
    val uploadedDate: String?,
    val expiryDate: String?,
    val createDate: String?,
    val updatedDate: String?,
    val email: Boolean = false,
    val whatsApp: Boolean = false,
    val sms: Boolean = false,
    val renewable: Boolean = false
) {
    fun hasValidFile(): Boolean {
        val hasStoredName = !s3FileName.isNullOrBlank()
        val hasDisplayName = !fileName.isNullOrBlank() && fileName != "No File Name"
        val hasLegacyLink = !s3Link.isNullOrBlank() && s3Link != "No Link Available"
        return uploaded && (hasStoredName || hasDisplayName || hasLegacyLink)
    }

    /** Server-stored file key — never the display/original fileName. */
    fun storedFileName(): String? {
        return s3FileName?.takeUnless { it.isBlank() }
            ?: serverFileKeyFromLink()
    }

    /** Human-readable name shown in the UI. */
    fun displayFileName(): String? {
        return fileName?.takeUnless { it.isBlank() || it == "No File Name" }
    }

    /** Download/preview URL — always derived from s3Link or s3FileName, never from fileName. */
    fun resolvePreviewUrl(userId: Long, baseUrl: String): String? {
        val link = s3Link?.takeUnless { it.isBlank() || it == "No Link Available" }
        if (!link.isNullOrBlank()) {
            return when {
                link.startsWith("http") -> rewriteHost(link, baseUrl)
                link.startsWith("/") -> "${baseUrl.trimEnd('/')}$link"
                else -> FileUrlBuilder.downloadUrl(baseUrl, userId, link)
            }
        }

        storedFileName()?.let { key ->
            return FileUrlBuilder.downloadUrl(baseUrl, userId, key)
        }
        return null
    }

    private fun serverFileKeyFromLink(): String? {
        return s3Link?.substringAfterLast('/')
            ?.takeUnless { it.isBlank() || it == "No Link Available" }
    }

    private fun rewriteHost(url: String, baseUrl: String): String {
        return try {
            val configured = java.net.URI(baseUrl)
            val original = java.net.URI(url)
            if (original.host == "localhost" || original.host == "127.0.0.1") {
                java.net.URI(
                    original.scheme,
                    original.userInfo,
                    configured.host,
                    if (configured.port != -1) configured.port else original.port,
                    original.path,
                    original.query,
                    original.fragment
                ).toString()
            } else {
                url
            }
        } catch (e: Exception) {
            url
        }
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

    /** Human-readable original name from the device (for optional UI only). */
    val originalDisplayName: String?
        get() = originalFileName?.takeIf { it.isNotBlank() }
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
    val isRenewable: Boolean get() = uploaded?.renewable ?: template.renewable
}
