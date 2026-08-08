package com.byebyechallan.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.model.VehicleSummary
import com.byebyechallan.app.data.remote.SessionManager
import com.byebyechallan.app.data.repository.ApiResult
import com.byebyechallan.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileDetailUiState(
    val isLoading: Boolean = true,
    val vehicles: List<VehicleSummary> = emptyList()
)

class ProfileDetailViewModel(
    private val profileId: Long,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
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
                is ApiResult.Success -> {
                    val vehicles = result.data.mapNotNull(VehicleSummary::fromDto)
                    _uiState.value = ProfileDetailUiState(isLoading = false, vehicles = vehicles)
                }
                is ApiResult.Error -> {
                    _uiState.value = ProfileDetailUiState(isLoading = false, vehicles = emptyList())
                }
            }
        }
    }
}
