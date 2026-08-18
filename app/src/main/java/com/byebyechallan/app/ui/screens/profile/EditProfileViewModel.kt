package com.byebyechallan.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.model.NotificationRecipientState
import com.byebyechallan.app.data.remote.SessionManager
import com.byebyechallan.app.data.repository.ApiResult
import com.byebyechallan.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val profileName: String? = null,
    val recipients: NotificationRecipientState? = null,
    val defaultEmail: String = "",
    val defaultMobile: String = "",
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class EditProfileViewModel(
    private val profileId: Long,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = EditProfileUiState(isLoading = true)
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.value = EditProfileUiState(
                    isLoading = false,
                    errorMessage = "Session expired. Please log in again."
                )
                return@launch
            }
            val email = sessionManager.getEmail().orEmpty()
            val mobile = sessionManager.getMobileNo().orEmpty()
            when (val result = profileRepository.getProfile(userId, profileId)) {
                is ApiResult.Success -> {
                    val profile = result.data
                    val recipientState = NotificationRecipientState.fromMap(
                        recipients = profile.resolvedNotificationRecipients(),
                        defaultEmail = email,
                        defaultMobile = mobile
                    )
                    _uiState.value = EditProfileUiState(
                        isLoading = false,
                        profileName = profile.profileName.orEmpty(),
                        recipients = recipientState,
                        defaultEmail = email,
                        defaultMobile = mobile
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = EditProfileUiState(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun updateProfile(profileName: String, recipients: NotificationRecipientState) {
        if (profileName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a profile name.")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Session expired. Please log in again."
                )
                return@launch
            }
            val result = profileRepository.updateProfile(
                userId = userId,
                profileId = profileId,
                profileName = profileName.trim(),
                recipients = recipients.toRecipientsMap()
            )
            _uiState.value = when (result) {
                is ApiResult.Success -> _uiState.value.copy(isLoading = false, isSuccess = true)
                is ApiResult.Error -> _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }
}
