package com.byebyechallan.app.data.model

data class ProfileRequestDto(
    val profileName: String
)

data class ProfileDto(
    val id: Long,
    val userId: Long,
    val profileName: String
)
