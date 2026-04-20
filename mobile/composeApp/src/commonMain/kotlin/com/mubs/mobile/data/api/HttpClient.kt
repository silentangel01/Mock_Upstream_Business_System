package com.mubs.mobile.data.api

import com.mubs.mobile.domain.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createHttpClient(sessionManager: SessionManager): HttpClient {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Logging) {
            level = LogLevel.HEADERS
        }

        defaultRequest {
            url(platformBaseUrl())
            contentType(ContentType.Application.Json)
        }
    }

    client.plugin(HttpSend).intercept { request ->
        val token = sessionManager.getToken()
        if (token != null) {
            request.headers.append("Authorization", "Bearer $token")
        }
        execute(request)
    }

    return client
}
