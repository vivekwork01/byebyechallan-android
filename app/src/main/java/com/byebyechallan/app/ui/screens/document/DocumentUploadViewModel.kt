package com.byebyechallan.app.ui.screens.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.model.DocumentRequestDto
import com.byebyechallan.app.data.model.NotificationChannel
import com.byebyechallan.app.data.model.NotificationRecipientState
import com.byebyechallan.app.data.model.UserDocumentDto
import com.byebyechallan.app.data.repository.ApiResult
import com.byebyechallan.app.data.repository.DocumentRepository
import com.byebyechallan.app.data.repository.ProfileRepository
import com.byebyechallan.app.util.DateUtils
import com.byebyechallan.app.util.RegistrationCertificateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

data class DocumentUploadUiState(
    val isLoadingExisting: Boolean = true,
    val existingDoc: UserDocumentDto? = null,
    val profileRecipients: Map<NotificationChannel, String>? = null,
    val profileRecipientsLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
) {
    fun isChannelAvailableOnProfile(channel: NotificationChannel): Boolean {
        if (!profileRecipientsLoaded) return false
        return NotificationRecipientState.isChannelAvailableOnProfile(profileRecipients, channel)
    }
}

class DocumentUploadViewModel(
    private val userId: Long,
    private val profileId: Long,
    private val vehicleRegNo: String,
    private val docTemplateId: String,
    private val docName: String,
    private val documentRepository: DocumentRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentUploadUiState())
    val uiState: StateFlow<DocumentUploadUiState> = _uiState

    init {
        loadExistingIfAny()
        loadProfileRecipients()
    }

    private fun loadProfileRecipients() {
        viewModelScope.launch {
            when (val result = profileRepository.getProfile(userId, profileId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        profileRecipients = result.data.resolvedNotificationRecipients(),
                        profileRecipientsLoaded = true
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(profileRecipientsLoaded = true)
                }
            }
        }
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
        val savedRenewable = _uiState.value.existingDoc?.renewable ?: isRenewable
        val isRc = RegistrationCertificateUtils.isRegistrationCertificate(docName)

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            var savedOriginalFileName = _uiState.value.existingDoc?.displayFileName()
            var savedS3FileName = _uiState.value.existingDoc?.storedFileName()
            var rcS3Link = _uiState.value.existingDoc?.s3Link
            if (file != null) {
                when (val uploadResult = documentRepository.uploadFile(userId, file)) {
                    is ApiResult.Success -> {
                        savedS3FileName = uploadResult.data.storedFileName
                            ?: uploadResult.data.fileUrl?.substringAfterLast('/')
                        savedOriginalFileName = uploadResult.data.originalDisplayName ?: file.name
                        rcS3Link = uploadResult.data.fileUrl
                            ?: uploadResult.data.filePath
                            ?: savedS3FileName
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = uploadResult.message)
                        return@launch
                    }
                }
            }

            val profileState = _uiState.value
            val emailAllowed = profileState.isChannelAvailableOnProfile(NotificationChannel.EMAIL)
            val whatsAppAllowed = profileState.isChannelAvailableOnProfile(NotificationChannel.WHATSAPP)
            val smsAllowed = profileState.isChannelAvailableOnProfile(NotificationChannel.SMS)
            val notifyViaEmail = emailAllowed && notifyEmail
            val notifyViaWhatsApp = whatsAppAllowed && notifyWhatsApp
            val notifyViaSms = smsAllowed && notifySms

            val request = DocumentRequestDto(
                id = _uiState.value.existingDoc?.id ?: 0L,
                docTemplateId = docTemplateId,
                docName = docName,
                docId = _uiState.value.existingDoc?.docId ?: "${docTemplateId}_${System.currentTimeMillis()}",
                expiryDate = expiryDate?.let { DateUtils.toIsoDateTimeString(it) },
                fileName = savedOriginalFileName,
                s3FileName = savedS3FileName,
                email = notifyViaEmail,
                whatsApp = notifyViaWhatsApp,
                sms = notifyViaSms,
                uploaded = true,
                renewable = savedRenewable
            )

            val rcDto = if (isRc) {
                com.byebyechallan.app.data.model.RCDto(
                    registrationNo = vehicleRegNo.trim().uppercase(),
                    registrationDate = null,
                    expiryDate = expiryDate?.let { DateUtils.toIsoDateTimeString(it) },
                    rcS3Link = rcS3Link
                )
            } else {
                null
            }

            val saveResult = documentRepository.saveDocument(userId, profileId, vehicleRegNo, request, rcDto)
            _uiState.value = when (saveResult) {
                is ApiResult.Success -> _uiState.value.copy(isSaving = false, isSuccess = true)
                is ApiResult.Error -> _uiState.value.copy(isSaving = false, errorMessage = saveResult.message)
            }
        }
    }
}
