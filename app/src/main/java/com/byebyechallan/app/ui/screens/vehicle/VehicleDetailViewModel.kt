package com.byebyechallan.app.ui.screens.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.model.DocumentChecklistItem
import com.byebyechallan.app.data.model.DocumentRequestDto
import com.byebyechallan.app.data.model.UserDocumentDto
import com.byebyechallan.app.data.remote.SessionManager
import com.byebyechallan.app.data.repository.ApiResult
import com.byebyechallan.app.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VehicleDetailUiState(
    val isLoading: Boolean = true,
    val items: List<DocumentChecklistItem> = emptyList(),
    val errorMessage: String? = null
)

class VehicleDetailViewModel(
    private val profileId: Long,
    private val vehicleRegNo: String,
    private val country: String,
    private val state: String,
    private val registrationType: String,
    private val vehicleType: String,
    private val documentRepository: DocumentRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleDetailUiState())
    val uiState: StateFlow<VehicleDetailUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.value = VehicleDetailUiState(errorMessage = "Session expired. Please log in again.")
                return@launch
            }

            if (country.isBlank() || state.isBlank() || registrationType.isBlank() || vehicleType.isBlank()) {
                when (val uploadedResult = documentRepository.getUploadedDocuments(userId, profileId, vehicleRegNo)) {
                    is ApiResult.Success -> {
                        _uiState.value = VehicleDetailUiState(
                            isLoading = false,
                            items = uploadedResult.data.map(::uploadedDocumentItem)
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = VehicleDetailUiState(errorMessage = uploadedResult.message, isLoading = false)
                    }
                }
                return@launch
            }

            val checklistResult = documentRepository.getDocumentChecklist(country, state, registrationType, vehicleType)
            val uploadedResult = documentRepository.getUploadedDocuments(userId, profileId, vehicleRegNo)

            if (checklistResult is ApiResult.Error) {
                _uiState.value = VehicleDetailUiState(errorMessage = checklistResult.message, isLoading = false)
                return@launch
            }
            if (uploadedResult is ApiResult.Error) {
                _uiState.value = VehicleDetailUiState(errorMessage = uploadedResult.message, isLoading = false)
                return@launch
            }

            val templates = (checklistResult as ApiResult.Success).data
            val uploaded = (uploadedResult as ApiResult.Success).data
            val merged = documentRepository.mergeChecklist(templates, uploaded)

            _uiState.value = VehicleDetailUiState(isLoading = false, items = merged)
        }
    }

    private fun uploadedDocumentItem(document: UserDocumentDto): DocumentChecklistItem {
        val templateId = document.docTemplateId ?: document.docId.orEmpty()
        return DocumentChecklistItem(
            template = DocumentRequestDto(
                docTemplateId = templateId,
                docName = document.docName ?: "Document",
                docId = templateId,
                expiryDate = document.expiryDate,
                notificationTime = document.notificationTime,
                email = document.email,
                whatsApp = document.whatsApp,
                sms = document.sms,
                uploaded = true,
                s3Link = document.s3Link
            ),
            uploaded = document
        )
    }
}
