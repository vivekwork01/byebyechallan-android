package com.byebyechallan.app.ui.screens.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.model.DocumentRequestDto
import com.byebyechallan.app.data.model.RCDto
import com.byebyechallan.app.data.model.VehicleRequestDto
import com.byebyechallan.app.data.model.VehicleSummary
import com.byebyechallan.app.data.model.CountryDto
import com.byebyechallan.app.data.model.RegistrationDto
import com.byebyechallan.app.data.model.StateDto
import com.byebyechallan.app.data.model.VehicleTypeResponseDto
import com.byebyechallan.app.data.repository.ApiResult
import com.byebyechallan.app.data.repository.DocumentRepository
import com.byebyechallan.app.data.repository.MasterRepository
import com.byebyechallan.app.data.repository.ProfileRepository
import com.byebyechallan.app.data.remote.SessionManager
import com.byebyechallan.app.util.DateUtils
import com.byebyechallan.app.util.RegistrationCertificateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime

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
    private val documentRepository: DocumentRepository,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
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
        rcFile: File?,
        registrationDate: java.time.LocalDate?,
        rcExpiryDate: java.time.LocalDate?,
        onSaved: (VehicleSummary) -> Unit
    ) {
        if (registrationNo.isBlank() || country.isBlank() || state.isBlank() ||
            registrationType.isBlank() || vehicleTypeId.isBlank()
        ) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all fields.")
            return
        }
        if (rcFile == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please upload the Registration Certificate.")
            return
        }
        if (registrationDate == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select the registration date.")
            return
        }
        if (rcExpiryDate == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select the RC expiry date.")
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = "Session expired. Please log in again.")
                return@launch
            }

            val normalizedRegNo = registrationNo.trim().uppercase()

            val uploadResult = documentRepository.uploadFile(userId, rcFile)
            val uploadData = when (uploadResult) {
                is ApiResult.Success -> uploadResult.data
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = uploadResult.message)
                    return@launch
                }
            }

            val savedS3FileName = uploadData.storedFileName
                ?: uploadData.fileUrl?.substringAfterLast('/')
            val savedOriginalFileName = uploadData.originalDisplayName ?: rcFile.name
            val rcS3Link = uploadData.fileUrl
                ?: uploadData.filePath
                ?: savedS3FileName

            val rcDto = RCDto(
                registrationNo = normalizedRegNo,
                registrationDate = DateUtils.toIsoDateTimeString(registrationDate),
                expiryDate = DateUtils.toIsoDateTimeString(rcExpiryDate),
                rcS3Link = rcS3Link
            )

            val checklistResult = documentRepository.getDocumentChecklist(country, state, registrationType, vehicleTypeId)
            val documents = when (checklistResult) {
                is ApiResult.Success -> checklistResult.data.map { template ->
                    val isRc = RegistrationCertificateUtils.isRegistrationCertificate(template.docName)
                    DocumentRequestDto(
                        id = template.id,
                        docTemplateId = template.docId,
                        docName = template.docName,
                        docId = if (isRc) "${template.docId}_${System.currentTimeMillis()}" else "",
                        expiryDate = if (isRc) DateUtils.toIsoDateTimeString(rcExpiryDate) else null,
                        notificationTime = if (isRc) LocalDateTime.now().toString() else template.notificationTime,
                        fileName = if (isRc) savedOriginalFileName else null,
                        s3FileName = if (isRc) savedS3FileName else null,
                        email = false,
                        whatsApp = false,
                        sms = false,
                        uploaded = isRc,
                        renewable = if (isRc) true else template.renewable
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

            val vehicleRequest = VehicleRequestDto(
                vehicleRegistrationNumber = normalizedRegNo,
                vehicleName = vehicleName.trim().ifEmpty { normalizedRegNo },
                rcDto = rcDto,
                documents = documents
            )

            val addResult = profileRepository.addVehicleToProfile(userId, profileId, normalizedRegNo, vehicleRequest)
            when (addResult) {
                is ApiResult.Success -> {
                    val vehicle = VehicleSummary(
                        registrationNo = normalizedRegNo,
                        vehicleName = vehicleName.trim().ifEmpty { normalizedRegNo },
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
