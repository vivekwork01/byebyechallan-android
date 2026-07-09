package com.byebyechallan.app.data.repository

import com.byebyechallan.app.data.model.ProfileDto
import com.byebyechallan.app.data.model.ProfileRequestDto
import com.byebyechallan.app.data.remote.ApiService

class ProfileRepository(private val api: ApiService) {

    suspend fun getAllProfiles(userId: Long): ApiResult<List<ProfileDto>> {
        return try {
            val response = api.getAllProfiles(userId)
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Couldn't load profiles (${response.code()}).")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error while loading profiles.")
        }
    }

    suspend fun createProfile(userId: Long, profileName: String): ApiResult<ProfileDto> {
        return try {
            val response = api.createProfile(userId, ProfileRequestDto(profileName))
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Couldn't create profile (${response.code()}).")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error while creating profile.")
        }
    }

    suspend fun addVehicleToProfile(
        userId: Long,
        profileId: Long,
        vehicleRegistrationNo: String,
        request: com.byebyechallan.app.data.model.VehicleRequestDto
    ): ApiResult<com.byebyechallan.app.data.model.ProfileVehicleResponseDto> {
        return try {
            val response = api.addVehicleToProfile(userId, profileId, vehicleRegistrationNo, request)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Couldn't add vehicle (${response.code()}).")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error while adding vehicle.")
        }
    }
}
