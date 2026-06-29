package com.byebyechallan.app.ui.screens.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.local.LocalVehicle
import com.byebyechallan.app.data.local.VehicleLocalStore
import com.byebyechallan.app.data.model.CountryDto
import com.byebyechallan.app.data.model.RegistrationDto
import com.byebyechallan.app.data.model.StateDto
import com.byebyechallan.app.data.repository.ApiResult
import com.byebyechallan.app.data.repository.MasterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * PLACEHOLDER DATA - PLEASE READ:
 * The Swagger spec has no endpoint to fetch vehicle types, even though
 * CoreVehicleTypeEntity exists as a schema and "vehicle_type" is a required
 * query param for GET /api/v1/document/list. This hardcoded list is a stand-in
 * so the app is usable now. RECOMMENDED FIX: backend should add
 * GET /api/v1/master/vehicle-type (or similar) returning real vehicleTypeId
 * values that match what's stored against CoreDocumentEntity records. Once that
 * exists, replace this list with a real repository call - same pattern as
 * countries/states below.
 */
val PLACEHOLDER_VEHICLE_TYPES = listOf("Two Wheeler", "Car", "Commercial Vehicle", "Truck", "Bus")

data class AddVehicleUiState(
    val countries: List<CountryDto> = emptyList(),
    val states: List<StateDto> = emptyList(),
    val registrationTypes: List<RegistrationDto> = emptyList(),
    val isLoadingMaster: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class AddVehicleViewModel(
    private val profileId: Long,
    private val masterRepository: MasterRepository,
    private val vehicleLocalStore: VehicleLocalStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddVehicleUiState())
    val uiState: StateFlow<AddVehicleUiState> = _uiState

    init {
        loadCountries()
    }

    private fun loadCountries() {
        viewModelScope.launch {
            when (val result = masterRepository.getCountries()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    countries = result.data,
                    isLoadingMaster = false
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    errorMessage = result.message,
                    isLoadingMaster = false
                )
            }
        }
    }

    fun onCountrySelected(countryId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(states = emptyList(), registrationTypes = emptyList())
            val statesResult = masterRepository.getStates(countryId)
            val regResult = masterRepository.getRegistrationTypes(countryId)
            _uiState.value = _uiState.value.copy(
                states = (statesResult as? ApiResult.Success)?.data ?: emptyList(),
                registrationTypes = (regResult as? ApiResult.Success)?.data ?: emptyList()
            )
        }
    }

    fun saveVehicle(
        registrationNo: String,
        country: String,
        state: String,
        registrationType: String,
        vehicleType: String,
        onSaved: (LocalVehicle) -> Unit
    ) {
        if (registrationNo.isBlank() || country.isBlank() || state.isBlank() ||
            registrationType.isBlank() || vehicleType.isBlank()
        ) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all fields.")
            return
        }
        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            val vehicle = LocalVehicle(
                profileId = profileId,
                registrationNo = registrationNo.trim().uppercase(),
                country = country,
                state = state,
                registrationType = registrationType,
                vehicleType = vehicleType
            )
            vehicleLocalStore.addVehicle(vehicle)
            _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
            onSaved(vehicle)
        }
    }
}
