package com.byebyechallan.app.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SplashDestination {
    object Loading : SplashDestination()
    object GoToHome : SplashDestination()
    object GoToLogin : SplashDestination()
}

class SplashViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination

    init {
        viewModelScope.launch {
            _destination.value = if (authRepository.isLoggedIn()) {
                SplashDestination.GoToHome
            } else {
                SplashDestination.GoToLogin
            }
        }
    }
}
