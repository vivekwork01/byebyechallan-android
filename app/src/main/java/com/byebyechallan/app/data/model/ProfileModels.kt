package com.byebyechallan.app.data.model

import com.google.gson.annotations.SerializedName

data class ProfileRequestDto(
    val profileName: String,
    val recipients: Map<NotificationChannel, String> = emptyMap()
)

data class ProfileDto(
    val id: Long,
    val userId: Long,
    val profileName: String?,
    val vehicleCount: Int = 0,
    /** Legacy/raw recipients payload returned by some profile list responses. */
    val recipients: String? = null,
    @SerializedName("notification_recipients")
    val notificationRecipients: Map<String, String>? = null
) {
    fun resolvedNotificationRecipients(): Map<NotificationChannel, String>? {
        if (!notificationRecipients.isNullOrEmpty()) {
            return NotificationRecipientState.normalizeStringMap(notificationRecipients)
        }
        if (!recipients.isNullOrBlank()) {
            return NotificationRecipientState.parseRecipientsJsonString(recipients)
        }
        return null
    }
}

// Request sent when adding a vehicle to a profile. The backend expects the
// registration number, optional vehicle name, and a list of document entries.
data class VehicleRequestDto(
    val vehicleRegistrationNumber: String,
    val vehicleName: String? = null,
    val rcDto: com.byebyechallan.app.data.model.RCDto? = null,
    val documents: List<com.byebyechallan.app.data.model.DocumentRequestDto> = emptyList()
)

// Response returned after adding a vehicle to a profile (includes created docs).
data class ProfileVehicleResponseDto(
    val id: Long,
    val profileVehicleName: String?,
    val vehicleRegistrationNo: String? = null,
    val docs: List<com.byebyechallan.app.data.model.UserDocumentDto> = emptyList()
)

/** UI/navigation model for a vehicle. Loaded from the server; not cached locally. */
data class VehicleSummary(
    val registrationNo: String,
    val vehicleName: String? = null,
    val country: String = "",
    val state: String = "",
    val registrationType: String = "",
    val vehicleType: String = ""
) {
    companion object {
        fun fromDto(dto: ProfileVehicleResponseDto): VehicleSummary? {
            val registrationNo = dto.vehicleRegistrationNo?.trim().orEmpty()
            if (registrationNo.isBlank()) return null
            return VehicleSummary(
                registrationNo = registrationNo,
                vehicleName = dto.profileVehicleName
            )
        }
    }
}
