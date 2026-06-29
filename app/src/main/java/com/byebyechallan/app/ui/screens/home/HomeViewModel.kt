package com.byebyechallan.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byebyechallan.app.data.local.VehicleLocalStore
import com.byebyechallan.app.data.model.ProfileDto
import com.byebyechallan.app.data.model.UserDocumentDto
import com.byebyechallan.app.data.remote.SessionManager
import com.byebyechallan.app.data.repository.ApiResult
import com.byebyechallan.app.data.repository.DocumentRepository
import com.byebyechallan.app.data.repository.ProfileRepository
import com.byebyechallan.app.util.DateUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileCardData(
    val profile: ProfileDto,
    val vehicleCount: Int,
    val soonestExpiringDoc: UserDocumentDto?
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val profiles: List<ProfileCardData> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val profileRepository: ProfileRepository,
    private val documentRepository: DocumentRepository,
    private val vehicleLocalStore: VehicleLocalStore,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.value = HomeUiState(errorMessage = "Session expired. Please log in again.")
                return@launch
            }

            when (val result = profileRepository.getAllProfiles(userId)) {
                is ApiResult.Success -> {
                    // For each profile, fetch its known vehicles (local cache) and
                    // their documents in parallel, then pick the soonest expiry.
                    val cardJobs = result.data.map { profile ->
                        async { buildProfileCard(userId, profile) }
                    }
                    val cards = cardJobs.awaitAll()
                    _uiState.value = HomeUiState(profiles = cards)
                }
                is ApiResult.Error -> {
                    _uiState.value = HomeUiState(errorMessage = result.message)
                }
            }
        }
    }

    private suspend fun buildProfileCard(userId: Long, profile: ProfileDto): ProfileCardData {
        val vehicles = vehicleLocalStore.getVehiclesForProfile(profile.id)
        val allDocs = mutableListOf<UserDocumentDto>()

        for (vehicle in vehicles) {
            val docsResult = documentRepository.getUploadedDocuments(userId, profile.id, vehicle.registrationNo)
            if (docsResult is ApiResult.Success) {
                allDocs.addAll(docsResult.data)
            }
        }

        val soonest = allDocs
            .filter { !it.expiryDate.isNullOrBlank() }
            .minByOrNull { DateUtils.parseToEpochMillis(it.expiryDate) ?: Long.MAX_VALUE }

        return ProfileCardData(
            profile = profile,
            vehicleCount = vehicles.size,
            soonestExpiringDoc = soonest
        )
    }
}
