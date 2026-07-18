package com.byebyechallan.app.data.model

import com.google.gson.annotations.SerializedName

data class ProfileRequestDto(
    val profileName: String
)

data class ProfileDto(
    val id: Long,

    @SerializedName("userId")
    val userId: Long,
    @SerializedName("profileName")
    val profileName: String?,
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
    @SerializedName("profileVehicleName")
    val profileVehicleName: String?,

    @SerializedName("vehicleRegistrationNo")
    val vehicleRegistrationNo: String? = null,

    @SerializedName("docs")
    val docs: List<com.byebyechallan.app.data.model.UserDocumentDto> = emptyList()
)
