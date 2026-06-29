package com.byebyechallan.app.data.model

// What the app sends when saving/updating a document record (after file is uploaded
// and we have an s3Link is NOT here - s3Link goes in via a separate field added
// server-side; see UploadResponse + this combined in the repository layer).
data class DocumentRequestDto(
    val docTemplateId: String,
    val docName: String,
    val docId: String,
    val expiryDate: String?,      // ISO date-time string, e.g. "2026-12-31T00:00:00"
    val notificationTime: String?,
    val email: Boolean = false,
    val whatsApp: Boolean = false,
    val sms: Boolean = false,
    val s3Link: String? = null    // populated after multipart upload completes
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
    val uploadedDate: String?,
    val expiryDate: String?,
    val notificationTime: String?,
    val createDate: String?,
    val updatedDate: String?,
    val email: Boolean = false,
    val whatsApp: Boolean = false,
    val sms: Boolean = false
)

// Template/checklist entry describing a document that SHOULD be uploaded
// for a given country/state/registration type/vehicle type combination.
data class CoreDocumentEntity(
    val docId: String,
    val docName: String,
    val registrationCode: String?,
    val docType: String?,          // e.g. "MANDATORY" / "OPTIONAL" - confirm exact values with backend
    val isDeleted: Boolean = false,
    val createdBy: Long? = null,
    val createdTime: String? = null,
    val countryStateEntity: CoreCountryStateEntity? = null,
    val vehicleTypeEntity: CoreVehicleTypeEntity? = null
)

data class CoreCountryStateEntity(
    val countryStateId: String?,
    val countryId: String?,
    val stateId: String?,
    val countryName: String?,
    val stateName: String?
)

data class CoreVehicleTypeEntity(
    val vehicleTypeId: String,
    val vehicleTypeName: String,
    val vehicleCategoryEntity: CoreVehicleCategoryEntity? = null
)

data class CoreVehicleCategoryEntity(
    val categoryId: String? = null,
    val categoryName: String? = null
)

// Response from the multipart file-upload endpoint (backend team to implement -
// see README "Backend To-Do" section)
data class UploadResponse(
    val s3Link: String
)

// A merged view used purely on the Android side: combines a checklist item
// (CoreDocumentEntity) with the matching uploaded record (UserDocumentDto) if one
// exists, so the Vehicle Detail screen can show "uploaded" vs "pending" in one list.
data class DocumentChecklistItem(
    val template: CoreDocumentEntity,
    val uploaded: UserDocumentDto?
) {
    val isUploaded: Boolean get() = uploaded != null
    val isMandatory: Boolean get() = template.docType?.equals("MANDATORY", ignoreCase = true) == true
}
