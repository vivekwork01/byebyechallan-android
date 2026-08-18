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

data class CreateProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val defaultRecipients: NotificationRecipientState? = null,
    val defaultEmail: String = "",
    val defaultMobile: String = ""
)

class CreateProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateProfileUiState())
    val uiState: StateFlow<CreateProfileUiState> = _uiState

    fun loadDefaultRecipients() {
        viewModelScope.launch {
            val email = sessionManager.getEmail().orEmpty()
            val mobile = sessionManager.getMobileNo().orEmpty()
            _uiState.value = _uiState.value.copy(
                defaultEmail = email,
                defaultMobile = mobile,
                defaultRecipients = NotificationRecipientState.defaultsFromUser(email, mobile)
            )
        }
    }

    fun createProfile(profileName: String, recipients: NotificationRecipientState) {
        if (profileName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a profile name.")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.value = CreateProfileUiState(errorMessage = "Session expired. Please log in again.")
                return@launch
            }
            val result = profileRepository.createProfile(
                userId = userId,
                profileName = profileName.trim(),
                recipients = recipients.toRecipientsMap()
            )
            _uiState.value = when (result) {
                is ApiResult.Success -> CreateProfileUiState(isSuccess = true)
                is ApiResult.Error -> CreateProfileUiState(errorMessage = result.message)
            }
        }
    }
}
