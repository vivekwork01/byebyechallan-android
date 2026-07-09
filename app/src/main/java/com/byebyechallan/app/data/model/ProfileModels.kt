package com.byebyechallan.app.data.model

import com.google.gson.annotations.SerializedName

data class ProfileRequestDto(
    val profileName: String
)

data class ProfileDto(
    val id: Long,
    val userId: Long,
    val profileName: String,
    @SerializedName("vehicleCount")
    val vehicleCount: Int = 0
)

// Request sent when adding a vehicle to a profile. The backend expects the
// registration number, optional vehicle name, and a list of document entries.
data class VehicleRequestDto(
    val vehicleRegistrationNumber: String,
    val vehicleName: String? = null,
    val documents: List<com.byebyechallan.app.data.model.DocumentRequestDto> = emptyList()
)

// Response returned after adding a vehicle to a profile (includes created docs).
data class ProfileVehicleResponseDto(
    val id: Long,
    val profileVehicleName: String?,
    // Backend may return different names for the registration field; include
    // multiple nullable properties and prefer the first non-null when mapping.
    val vehicleRegistrationNo: String? = null,
    val vehicleRegistrationNumber: String? = null,
    val registrationNo: String? = null,
    val docs: List<com.byebyechallan.app.data.model.UserDocumentDto> = emptyList()
)

