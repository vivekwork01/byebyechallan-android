package com.byebyechallan.app.ui.screens.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.model.VehicleSummary
import com.byebyechallan.app.data.model.CountryDto
import com.byebyechallan.app.data.model.RegistrationDto
import com.byebyechallan.app.data.model.StateDto
import com.byebyechallan.app.data.model.VehicleTypeResponseDto
import com.byebyechallan.app.data.repository.ApiResult
import com.byebyechallan.app.data.repository.MasterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Now uses the backend vehicle-type endpoint (GET /api/v1/master/vehicle-type).
 */

data class AddVehicleUiState(
    val countries: List<CountryDto> = emptyList(),
    val states: List<StateDto> = emptyList(),
    val registrationTypes: List<RegistrationDto> = emptyList(),
    val vehicleTypes: List<VehicleTypeResponseDto> = emptyList(),
    val isLoadingMaster: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class AddVehicleViewModel(
    private val profileId: Long,
    private val masterRepository: MasterRepository,
    private val documentRepository: com.byebyechallan.app.data.repository.DocumentRepository,
    private val profileRepository: com.byebyechallan.app.data.repository.ProfileRepository,
    private val sessionManager: com.byebyechallan.app.data.remote.SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddVehicleUiState())
    val uiState: StateFlow<AddVehicleUiState> = _uiState

    init {
        loadCountries()
        loadVehicleTypes()
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

    private fun loadVehicleTypes() {
        viewModelScope.launch {
            when (val result = masterRepository.getVehicleTypes()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(vehicleTypes = result.data)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
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
        vehicleName: String,
        country: String,
        state: String,
        registrationType: String,
        vehicleTypeId: String,
        onSaved: (VehicleSummary) -> Unit
    ) {
        if (registrationNo.isBlank() || country.isBlank() || state.isBlank() ||
            registrationType.isBlank() || vehicleTypeId.isBlank()
        ) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all fields.")
            return
        }
        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = "Session expired. Please log in again.")
                return@launch
            }

            // 1) Fetch required document templates for this registration (use vehicleTypeName for the query param)
            val checklistResult = documentRepository.getDocumentChecklist(country, state, registrationType, vehicleTypeId)
            val documents = when (checklistResult) {
                is ApiResult.Success -> checklistResult.data.map { template ->
                    com.byebyechallan.app.data.model.DocumentRequestDto(
                        id=template.id,
                        docTemplateId = template.docId,
                        docName = template.docName,
                        docId = "",
                        expiryDate = null,
                        notificationTime = template.notificationTime,
                        email = false,
                        whatsApp = false,
                        sms = false,
                        uploaded = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = checklistResult.message)
                    return@launch
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = "Unknown error while loading documents.")
                    return@launch
                }
            }

            // 2) Call backend to add vehicle to profile with prepared documents and vehicleTypeId
            val vehicleRequest = com.byebyechallan.app.data.model.VehicleRequestDto(
                vehicleRegistrationNumber = registrationNo.trim().uppercase(),
                vehicleName = vehicleName.trim().ifEmpty { registrationNo.trim().uppercase() },
                documents = documents
            )

            val addResult = profileRepository.addVehicleToProfile(userId, profileId, registrationNo.trim().uppercase(), vehicleRequest)
            when (addResult) {
                is ApiResult.Success -> {
                    val vehicle = VehicleSummary(
                        registrationNo = registrationNo.trim().uppercase(),
                        vehicleName = vehicleName.trim().ifEmpty { registrationNo.trim().uppercase() },
                        country = country,
                        state = state,
                        registrationType = registrationType,
                        vehicleType = vehicleTypeId
                    )
                    _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
                    onSaved(vehicle)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = addResult.message)
                }
            }
        }
    }
}
