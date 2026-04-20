package com.mubs.mobile.data.repository

import com.mubs.mobile.data.api.AuthApi
import com.mubs.mobile.data.model.LoginResponse
import com.mubs.mobile.domain.SessionManager

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) {
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return authApi.login(username, password).onSuccess { response ->
            sessionManager.saveToken(response.token)
            sessionManager.saveUsername(response.username)
            sessionManager.saveRole(response.role)
        }
    }

    suspend fun logout() {
        sessionManager.clear()
    }

    suspend fun isLoggedIn(): Boolean = sessionManager.getToken() != null

    suspend fun getRole(): String? = sessionManager.getRole()
}
