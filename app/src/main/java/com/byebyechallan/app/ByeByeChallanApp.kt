package com.byebyechallan.app

import android.app.Application
import com.byebyechallan.app.data.local.VehicleLocalStore
import com.byebyechallan.app.data.remote.ApiService
import com.byebyechallan.app.data.remote.RetrofitClient
import com.byebyechallan.app.data.remote.SessionManager
import com.byebyechallan.app.data.repository.AuthRepository
import com.byebyechallan.app.data.repository.DocumentRepository
import com.byebyechallan.app.data.repository.MasterRepository
import com.byebyechallan.app.data.repository.ProfileRepository

/**
 * Simple manual dependency container. For a project this size, a full DI
 * framework (Hilt/Dagger) would be overkill - this keeps things easy to follow
 * for someone new to Android. Everything is created once and reused app-wide.
 */
class ByeByeChallanApp : Application() {

    lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService

    lateinit var authRepository: AuthRepository
    lateinit var profileRepository: ProfileRepository
    lateinit var documentRepository: DocumentRepository
    lateinit var masterRepository: MasterRepository
    lateinit var vehicleLocalStore: VehicleLocalStore

    override fun onCreate() {
        super.onCreate()

        sessionManager = SessionManager(this)
        apiService = RetrofitClient.create(sessionManager)

        authRepository = AuthRepository(apiService, sessionManager)
        profileRepository = ProfileRepository(apiService)
        documentRepository = DocumentRepository(apiService)
        masterRepository = MasterRepository(apiService)
        vehicleLocalStore = VehicleLocalStore(this)
    }
}
