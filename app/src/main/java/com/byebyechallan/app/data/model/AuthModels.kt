package com.byebyechallan.app.data.model

// ---- Auth ----

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val mobileNo: String
)

data class LoginRequest(
    // Backend field is named "username" but the app collects the user's email
    // and sends it here. See README for context on this naming.
    val username: String,
    val password: String
)

data class AuthResponse(
    val token: String?,
    val refreshToken: String?,
    val message: String?
)
