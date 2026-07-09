package com.byebyechallan.app.data.model

data class CountryDto(
    val countryId: String,
    val countryStateId: String?,
    val countryName: String
)

data class StateDto(
    val countryStateId: String?,
    val countryId: String,
    val stateId: String,
    val countryName: String?,
    val stateName: String
)

data class RegistrationDto(
    val registrationCode: String,
    val registrationType: String
)

data class VehicleTypeResponseDto(
    val vehicleTypeId: String,
    val vehicleTypeName: String
)
