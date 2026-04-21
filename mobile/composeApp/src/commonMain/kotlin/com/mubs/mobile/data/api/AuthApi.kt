package com.mubs.mobile.data.api

import com.mubs.mobile.data.model.LoginRequest
import com.mubs.mobile.data.model.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText

class AuthApi(private val client: HttpClient) {

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return runCatching {
            val response = client.post("/api/auth/login") {
                setBody(LoginRequest(username, password))
            }
            if (response.status.value in 200..299) {
                response.body<LoginResponse>()
            } else {
                val errorBody = runCatching { response.bodyAsText() }.getOrDefault("")
                throw Exception("Login failed (${response.status.value}): $errorBody")
            }
        }
    }
}
