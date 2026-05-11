package com.mubs.mobile.data.api

import com.mubs.mobile.data.model.Fieldworker
import com.mubs.mobile.data.model.PageResponse
import com.mubs.mobile.data.model.ReassignRequest
import com.mubs.mobile.data.model.Ticket
import com.mubs.mobile.data.model.TicketStatus
import com.mubs.mobile.data.model.TicketStatusUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class TicketApi(private val client: HttpClient) {

    suspend fun listTickets(
        status: TicketStatus? = null,
        page: Int = 0,
        size: Int = 20
    ): Result<PageResponse<Ticket>> {
        return runCatching {
            client.get("/api/tickets") {
                status?.let { parameter("status", it.name) }
                parameter("page", page)
                parameter("size", size)
            }.bodyOrThrow()
        }
    }

    suspend fun getTicket(id: String): Result<Ticket> {
        return runCatching {
            client.get("/api/tickets/$id").bodyOrThrow()
        }
    }

    suspend fun updateStatus(
        id: String,
        status: String,
        note: String? = null
    ): Result<Ticket> {
        return runCatching {
            client.patch("/api/tickets/$id/status") {
                setBody(TicketStatusUpdateRequest(status, note))
            }.bodyOrThrow()
        }
    }

    suspend fun reassign(
        id: String,
        targetUser: String,
        note: String? = null
    ): Result<Ticket> {
        return runCatching {
            client.patch("/api/tickets/$id/reassign") {
                setBody(ReassignRequest(targetUser, note))
            }.bodyOrThrow()
        }
    }

    suspend fun listFieldworkers(team: String? = null): Result<List<Fieldworker>> {
        return runCatching {
            client.get("/api/tickets/fieldworkers") {
                team?.let { parameter("team", it) }
            }.bodyOrThrow()
        }
    }

    suspend fun uploadPhoto(
        id: String,
        fileName: String,
        fileBytes: ByteArray
    ): Result<Map<String, String>> {
        return runCatching {
            client.submitFormWithBinaryData(
                url = "/api/tickets/$id/photos",
                formData = formData {
                    append("file", fileBytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        append(HttpHeaders.ContentType, "image/jpeg")
                    })
                }
            ).bodyOrThrow()
        }
    }

    suspend fun deletePhoto(id: String, photoUrl: String): Result<Map<String, String>> {
        return runCatching {
            client.delete("/api/tickets/$id/photos") {
                parameter("url", photoUrl)
            }.bodyOrThrow()
        }
    }

    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
        val statusCode = status.value
        if (statusCode in 200..299) {
            return body()
        }

        val errorBody = runCatching { bodyAsText() }.getOrDefault("").trim()
        val message = when (statusCode) {
            401, 403 -> "Session expired. Please log in again."
            404 -> "Resource not found."
            else -> errorBody.ifBlank { "Request failed ($statusCode)." }
        }
        throw ApiException(statusCode, message)
    }
}
