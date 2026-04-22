package com.mubs.mobile.data.repository

import com.mubs.mobile.data.api.TicketApi
import com.mubs.mobile.data.model.Fieldworker
import com.mubs.mobile.data.model.PageResponse
import com.mubs.mobile.data.model.Ticket
import com.mubs.mobile.data.model.TicketStatus

class TicketRepository(private val ticketApi: TicketApi) {

    suspend fun listTickets(
        status: TicketStatus? = null,
        page: Int = 0,
        size: Int = 20
    ): Result<PageResponse<Ticket>> = ticketApi.listTickets(status, page, size)

    suspend fun getTicket(id: String): Result<Ticket> = ticketApi.getTicket(id)

    suspend fun updateStatus(
        id: String,
        status: String,
        note: String? = null
    ): Result<Ticket> = ticketApi.updateStatus(id, status, note)

    suspend fun reassign(
        id: String,
        targetUser: String,
        note: String? = null
    ): Result<Ticket> = ticketApi.reassign(id, targetUser, note)

    suspend fun listFieldworkers(team: String? = null): Result<List<Fieldworker>> =
        ticketApi.listFieldworkers(team)

    suspend fun uploadPhoto(
        id: String,
        fileName: String,
        fileBytes: ByteArray
    ): Result<Map<String, String>> = ticketApi.uploadPhoto(id, fileName, fileBytes)

    suspend fun deletePhoto(
        id: String,
        photoUrl: String
    ): Result<Map<String, String>> = ticketApi.deletePhoto(id, photoUrl)
}
