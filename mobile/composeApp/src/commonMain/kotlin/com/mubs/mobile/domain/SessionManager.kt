package com.mubs.mobile.domain

expect class SessionManager {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    suspend fun saveUsername(username: String)
    suspend fun getUsername(): String?
    suspend fun saveRole(role: String)
    suspend fun getRole(): String?
    suspend fun clear()
}
