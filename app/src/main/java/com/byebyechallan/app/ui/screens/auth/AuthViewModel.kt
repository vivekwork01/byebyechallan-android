package com.byebyechallan.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.repository.ApiResult
import com.byebyechallan.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow(AuthUiState())
    val loginState: StateFlow<AuthUiState> = _loginState

    private val _registerState = MutableStateFlow(AuthUiState())
    val registerState: StateFlow<AuthUiState> = _registerState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = AuthUiState(errorMessage = "Please enter both email and password.")
            return
        }
        _loginState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            val result = authRepository.login(email.trim(), password)
            _loginState.value = when (result) {
                is ApiResult.Success -> AuthUiState(isSuccess = true)
                is ApiResult.Error -> AuthUiState(errorMessage = result.message)
            }
        }
    }

    fun register(name: String, email: String, password: String, mobileNo: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank() || mobileNo.isBlank()) {
            _registerState.value = AuthUiState(errorMessage = "Please fill in all fields.")
            return
        }
        if (password.length < 6) {
            _registerState.value = AuthUiState(errorMessage = "Password should be at least 6 characters.")
            return
        }
        _registerState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            val result = authRepository.register(name.trim(), email.trim(), password, mobileNo.trim())
            _registerState.value = when (result) {
                is ApiResult.Success -> AuthUiState(isSuccess = true)
                is ApiResult.Error -> AuthUiState(errorMessage = result.message)
            }
        }
    }

    fun clearRegisterState() {
        _registerState.value = AuthUiState()
    }
}
