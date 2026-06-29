package com.byebyechallan.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.local.LocalVehicle
import com.byebyechallan.app.data.local.VehicleLocalStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileDetailUiState(
    val isLoading: Boolean = true,
    val vehicles: List<LocalVehicle> = emptyList()
)

class ProfileDetailViewModel(
    private val profileId: Long,
    private val vehicleLocalStore: VehicleLocalStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileDetailUiState())
    val uiState: StateFlow<ProfileDetailUiState> = _uiState

    init {
        loadVehicles()
    }

    fun loadVehicles() {
        viewModelScope.launch {
            val vehicles = vehicleLocalStore.getVehiclesForProfile(profileId)
            _uiState.value = ProfileDetailUiState(isLoading = false, vehicles = vehicles)
        }
    }
}
