package com.byebyechallan.app.data.model

import com.google.gson.annotations.SerializedName

// What the app sends when saving/updating a document record.
data class DocumentRequestDto(
    val docTemplateId: String,
    val docName: String,
    val docId: String,
    val expiryDate: String?,      // ISO date-time string, e.g. "2026-12-31T00:00:00"
    val notificationTime: String?,
    val email: Boolean = false,
    val whatsApp: Boolean = false,
    val sms: Boolean = false,
    val uploaded: Boolean = false,
    val s3Link: String? = null    // populated from UploadResponse after file upload completes
)

// What the backend returns for a saved/existing document
data class UserDocumentDto(
    val id: Long,

    @SerializedName("userId")
    val userId: Long,

    @SerializedName("profileId")
    val profileId: Long,

    @SerializedName("vehicleRegistrationNo")
    val vehicleRegistrationNo: String,

    @SerializedName("docTemplateId")
    val docTemplateId: String?,

    @SerializedName("docId")
    val docId: String?,

    @SerializedName("docName")
    val docName: String?,

    @SerializedName("s3Link")
    val s3Link: String?,

    @SerializedName("uploadedDate")
    val uploadedDate: String?,

    @SerializedName("expiryDate")
    val expiryDate: String?,

    @SerializedName("notificationTime")
    val notificationTime: String?,

    @SerializedName("createDate")
    val createDate: String?,

    @SerializedName("updatedDate")
    val updatedDate: String?,

    @SerializedName("email")
    val email: Boolean = false,

    @SerializedName("whatsApp")
    val whatsApp: Boolean = false,

    @SerializedName("sms")
    val sms: Boolean = false
)


data class UploadResponse(
    val fileName: String? = null,
    val filePath: String? = null,
    val fileUrl: String? = null
) {
    val s3Link: String?
        get() = fileUrl ?: filePath
}

// A merged view used on the Android side: combines a checklist item (what the backend returns)
// with the matching uploaded record (UserDocumentDto) if one exists, so the Vehicle Detail screen
// can show "uploaded" vs "pending" in one list.
data class DocumentChecklistItem(
    val template: DocumentRequestDto,
    val uploaded: UserDocumentDto?
) {
    val isUploaded: Boolean get() = uploaded != null
    // Backend no longer returns a docType field in the checklist/template objects per OpenAPI.
    // isMandatory defaults to false; UI should be updated when the backend adds a field to indicate mandatory.
    val isMandatory: Boolean get() = false
}
