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
        notifySms: Boolean
    ) {
        val hasExistingFile = _uiState.value.existingDoc?.s3Link != null
        if (file == null && !hasExistingFile) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a file to upload.")
            return
        }
        if (expiryDate == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select an expiry date.")
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            // Step 1: upload the new file if one was picked. If editing and no new
            // file was picked, keep the existing s3Link untouched.
            var s3Link = _uiState.value.existingDoc?.s3Link
            if (file != null) {
                when (val uploadResult = documentRepository.uploadFile(file)) {
                    is ApiResult.Success -> s3Link = uploadResult.data.s3Link
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = uploadResult.message)
                        return@launch
                    }
                }
            }

            // Step 2: save/update the document record.
            // NOTE: docId here is a generated identifier for this document instance,
            // distinct from docTemplateId (which refers to the checklist template).
            // The exact intended meaning of "docId" vs "docTemplateId" in the backend
            // schema is ambiguous from the Swagger spec alone - confirm with backend
            // and adjust if this assumption is wrong.
            val request = DocumentRequestDto(
                docTemplateId = docTemplateId,
                docName = docName,
                docId = _uiState.value.existingDoc?.docId ?: "${docTemplateId}_${System.currentTimeMillis()}",
                expiryDate = LocalDateTime.now().toString(),
                notificationTime = LocalDateTime.now().toString(),
                email = notifyEmail,
                whatsApp = notifyWhatsApp,
                sms = notifySms,
                s3Link = s3Link
            )

            val saveResult = documentRepository.saveDocument(userId, profileId, vehicleRegNo, request)
            _uiState.value = when (saveResult) {
                is ApiResult.Success -> _uiState.value.copy(isSaving = false, isSuccess = true)
                is ApiResult.Error -> _uiState.value.copy(isSaving = false, errorMessage = saveResult.message)
            }
        }
    }
}
