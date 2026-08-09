package com.byebyechallan.app.ui.screens.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.model.DocumentRequestDto
import com.byebyechallan.app.data.model.UserDocumentDto
import com.byebyechallan.app.data.repository.ApiResult
import com.byebyechallan.app.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.LocalDate

data class DocumentUploadUiState(
    val isLoadingExisting: Boolean = true,
    val existingDoc: UserDocumentDto? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class DocumentUploadViewModel(
    private val userId: Long,
    private val profileId: Long,
    private val vehicleRegNo: String,
    private val docTemplateId: String,
    private val docName: String,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentUploadUiState())
    val uiState: StateFlow<DocumentUploadUiState> = _uiState

    init {
        loadExistingIfAny()
    }

    /** Checks whether this document type already has an uploaded record, to support "edit" mode. */
    private fun loadExistingIfAny() {
        viewModelScope.launch {
            val result = documentRepository.getUploadedDocuments(userId, profileId, vehicleRegNo)
            val existing = (result as? ApiResult.Success)?.data?.find { it.docTemplateId == docTemplateId }
            _uiState.value = _uiState.value.copy(isLoadingExisting = false, existingDoc = existing)
        }
    }

    fun submit(
        file: File?,
        expiryDate: LocalDate?,
        notifyEmail: Boolean,
        notifyWhatsApp: Boolean,
        notifySms: Boolean,
        isRenewable: Boolean
    ) {
        val hasExistingFile = _uiState.value.existingDoc?.hasValidFile() == true
        if (file == null && !hasExistingFile) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a file to upload.")
            return
        }
        if (isRenewable && expiryDate == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select an expiry date.")
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            var savedOriginalFileName = _uiState.value.existingDoc?.displayFileName()
            var savedS3FileName = _uiState.value.existingDoc?.storedFileName()
            if (file != null) {
                when (val uploadResult = documentRepository.uploadFile(userId, file)) {
                    is ApiResult.Success -> {
                        savedS3FileName = uploadResult.data.storedFileName
                            ?: uploadResult.data.fileUrl?.substringAfterLast('/')
                        savedOriginalFileName = uploadResult.data.originalDisplayName ?: file.name
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = uploadResult.message)
                        return@launch
                    }
                }
            }

            val request = DocumentRequestDto(
                id = _uiState.value.existingDoc?.id ?: 0L,
                docTemplateId = docTemplateId,
                docName = docName,
                docId = _uiState.value.existingDoc?.docId ?: "${docTemplateId}_${System.currentTimeMillis()}",
                expiryDate = if (isRenewable) expiryDate?.atStartOfDay()?.toString() else null,
                notificationTime = if (isRenewable) LocalDateTime.now().toString() else null,
                fileName = savedOriginalFileName,
                s3FileName = savedS3FileName,
                email = if (isRenewable) notifyEmail else false,
                whatsApp = if (isRenewable) notifyWhatsApp else false,
                sms = if (isRenewable) notifySms else false,
                uploaded = true,
                renewable = isRenewable
            )

            val saveResult = documentRepository.saveDocument(userId, profileId, vehicleRegNo, request)
            _uiState.value = when (saveResult) {
                is ApiResult.Success -> _uiState.value.copy(isSaving = false, isSuccess = true)
                is ApiResult.Error -> _uiState.value.copy(isSaving = false, errorMessage = saveResult.message)
            }
        }
    }
}
