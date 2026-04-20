package com.mubs.mobile.data.api

import com.mubs.mobile.data.model.LoginRequest
import com.mubs.mobile.data.model.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class AuthApi(private val client: HttpClient) {

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return runCatching {
            client.post("/api/auth/login") {
                setBody(LoginRequest(username, password))
            }.body<LoginResponse>()
        }
    }
}
