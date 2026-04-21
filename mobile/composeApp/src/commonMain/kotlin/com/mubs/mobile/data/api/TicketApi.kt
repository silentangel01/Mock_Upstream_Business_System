package com.mubs.mobile.data.api

import com.mubs.mobile.data.model.Fieldworker
import com.mubs.mobile.data.model.PageResponse
import com.mubs.mobile.data.model.ReassignRequest
import com.mubs.mobile.data.model.Ticket
import com.mubs.mobile.data.model.TicketStatus
import com.mubs.mobile.data.model.TicketStatusUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
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
            }.body()
        }
    }

    suspend fun getTicket(id: String): Result<Ticket> {
        return runCatching {
            client.get("/api/tickets/$id").body()
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
            }.body()
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
            }.body()
        }
    }

    suspend fun listFieldworkers(team: String? = null): Result<List<Fieldworker>> {
        return runCatching {
            client.get("/api/tickets/fieldworkers") {
                team?.let { parameter("team", it) }
            }.body()
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
            ).body()
        }
    }
}
