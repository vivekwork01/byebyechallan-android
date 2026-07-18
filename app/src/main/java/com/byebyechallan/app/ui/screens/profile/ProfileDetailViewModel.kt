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
    private val sessionManager: com.byebyechallan.app.data.remote.SessionManager,
    private val vehicleLocalStore: com.byebyechallan.app.data.local.VehicleLocalStore
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
                    val cachedVehicles = vehicleLocalStore.getVehiclesForProfile(profileId)
                    val cachedByRegistration = cachedVehicles.associateBy { it.registrationNo }
                    val vehicles = result.data.mapNotNull { dto ->
                        val registrationNo = dto.vehicleRegistrationNo ?: ""
                        if (registrationNo.isBlank()) {
                            return@mapNotNull null
                        }
                        val cached = cachedByRegistration[registrationNo]
                        cached?.copy(vehicleName = dto.profileVehicleName ?: cached.vehicleName)
                            ?: com.byebyechallan.app.data.local.LocalVehicle(
                                profileId = profileId,
                                registrationNo = registrationNo,
                                country = "",
                                state = "",
                                registrationType = "",
                                vehicleType = "",
                                vehicleName = dto.profileVehicleName
                            )
                    }
                    val serverRegistrations = vehicles.map { it.registrationNo }.toSet()
                    _uiState.value = ProfileDetailUiState(
                        isLoading = false,
                        vehicles = vehicles + cachedVehicles.filter { it.registrationNo !in serverRegistrations }
                    )
                }
                is com.byebyechallan.app.data.repository.ApiResult.Error -> {
                    _uiState.value = ProfileDetailUiState(isLoading = false, vehicles = emptyList())
                }
            }
        }
    }
}
