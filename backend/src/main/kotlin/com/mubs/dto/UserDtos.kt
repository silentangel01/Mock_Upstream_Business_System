package com.mubs.dto

import com.mubs.model.enums.UserRole

data class UserResponse(
    val id: String,
    val username: String,
    val role: UserRole,
    val team: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val displayName: String? = null,
    val enabled: Boolean = true
)

data class CreateUserRequest(
    val username: String,
    val password: String,
    val role: UserRole,
    val team: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val displayName: String? = null
)

data class UpdateUserRequest(
    val email: String? = null,
    val phone: String? = null,
    val displayName: String? = null,
    val team: String? = null,
    val role: UserRole? = null,
    val enabled: Boolean? = null
)

data class ResetPasswordRequest(
    val newPassword: String
)
