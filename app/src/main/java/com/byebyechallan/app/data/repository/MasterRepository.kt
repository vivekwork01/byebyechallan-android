package com.byebyechallan.app.data.repository

import com.byebyechallan.app.data.model.CountryDto
import com.byebyechallan.app.data.model.RegistrationDto
import com.byebyechallan.app.data.model.StateDto
import com.byebyechallan.app.data.remote.ApiService

class MasterRepository(private val api: ApiService) {

    suspend fun getCountries(): ApiResult<List<CountryDto>> {
        return try {
            val response = api.getCountries()
            if (response.isSuccessful) ApiResult.Success(response.body() ?: emptyList())
            else ApiResult.Error("Couldn't load countries (${response.code()}).")
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error while loading countries.")
        }
    }

    suspend fun getStates(countryId: String): ApiResult<List<StateDto>> {
        return try {
            val response = api.getStates(countryId)
            if (response.isSuccessful) ApiResult.Success(response.body() ?: emptyList())
            else ApiResult.Error("Couldn't load states (${response.code()}).")
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error while loading states.")
        }
    }

    suspend fun getRegistrationTypes(countryId: String): ApiResult<List<RegistrationDto>> {
        return try {
            val response = api.getRegistrationTypes(countryId)
            if (response.isSuccessful) ApiResult.Success(response.body() ?: emptyList())
            else ApiResult.Error("Couldn't load registration types (${response.code()}).")
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error while loading registration types.")
        }
    }
}
