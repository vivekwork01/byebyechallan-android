package com.byebyechallan.app.data.repository

import com.byebyechallan.app.data.model.AuthResponse
import com.byebyechallan.app.data.model.LoginRequest
import com.byebyechallan.app.data.model.RegisterRequest
import com.byebyechallan.app.data.remote.ApiService
import com.byebyechallan.app.data.remote.SessionManager
import com.byebyechallan.app.util.JwtUtils

class AuthRepository(
    private val api: ApiService,
    private val sessionManager: SessionManager
) {

    suspend fun register(name: String, email: String, password: String, mobileNo: String): ApiResult<Unit> {
        return try {
            val response = api.register(RegisterRequest(name, email, password, mobileNo))
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.errorBody()?.string() ?: "Registration failed. Please try again.")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error. Is the backend running and reachable?")
        }
    }

    /**
     * Logs in with email (sent as "username" per backend contract), saves the
     * token, and decodes the userId from the JWT (see JwtUtils for why).
     */
    suspend fun login(email: String, password: String): ApiResult<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(username = email, password = password))
            if (response.isSuccessful && response.body()?.token != null) {
                val auth = response.body()!!
                val userId = JwtUtils.extractUserId(auth.token!!)
                print(response.body())
                println("12344")
                if (userId == null) {
                    return ApiResult.Error(
                        "Logged in, but couldn't determine your user ID from the server response. " +
                            "Backend needs to add a userId field to the login response."
                    )
                }
                sessionManager.saveSession(auth.token, auth.refreshToken, userId)
                ApiResult.Success(auth)
            } else {
                ApiResult.Error(response.body()?.message ?: "Invalid email or password.")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error. Is the backend running and reachable?")
        }
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    suspend fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    suspend fun getCurrentUserId(): Long? = sessionManager.getUserId()
}
