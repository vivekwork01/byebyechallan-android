package com.byebyechallan.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.local.LocalVehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileDetailUiState(
    val isLoading: Boolean = true,
    val vehicles: List<LocalVehicle> = emptyList()
)

class ProfileDetailViewModel(
    private val profileId: Long,
    private val profileRepository: com.byebyechallan.app.data.repository.ProfileRepository,
    private val sessionManager: com.byebyechallan.app.data.remote.SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileDetailUiState())
    val uiState: StateFlow<ProfileDetailUiState> = _uiState

    init {
        loadVehicles()
    }

    fun loadVehicles() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.value = ProfileDetailUiState(isLoading = false, vehicles = emptyList())
                return@launch
            }

            when (val result = profileRepository.getVehiclesForProfile(userId, profileId)) {
                is com.byebyechallan.app.data.repository.ApiResult.Success -> {
                    val vehicles = result.data.map { dto ->
                        com.byebyechallan.app.data.local.LocalVehicle(
                            profileId = profileId,
                            registrationNo = dto.vehicleRegistrationNo ?: "",
                            country = "",
                            state = "",
                            registrationType = "",
                            vehicleType = "",
                            vehicleName = dto.profileVehicleName
                        )
                    }
                    _uiState.value = ProfileDetailUiState(isLoading = false, vehicles = vehicles)
                }
                is com.byebyechallan.app.data.repository.ApiResult.Error -> {
                    _uiState.value = ProfileDetailUiState(isLoading = false, vehicles = emptyList())
                }
            }
        }
    }
}
