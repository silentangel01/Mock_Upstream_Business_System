package com.mubs.controller

import com.mubs.dto.ReassignRequest
import com.mubs.dto.TicketFilterParams
import com.mubs.dto.TicketStatusUpdateRequest
import com.mubs.dto.TicketStatsResponse
import com.mubs.model.Ticket
import com.mubs.model.enums.TicketStatus
import com.mubs.service.NotificationService
import com.mubs.service.TicketService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tickets")
class TicketController(
    private val ticketService: TicketService,
    private val notificationService: NotificationService
) {

    @GetMapping
    fun listTickets(
        @RequestParam(required = false) status: TicketStatus?,
        @RequestParam(required = false) eventType: String?,
        @RequestParam(required = false) assignedTeam: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<Ticket>> {
        val params = TicketFilterParams(
            status = status,
            eventType = eventType,
            assignedTeam = assignedTeam,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ticketService.findAll(params))
    }

    @GetMapping("/{id}")
    fun getTicket(@PathVariable id: String): ResponseEntity<Ticket> {
        val ticket = ticketService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ticket)
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: String,
        @Valid @RequestBody request: TicketStatusUpdateRequest,
        authentication: Authentication
    ): ResponseEntity<Ticket> {
        val updated = ticketService.updateStatus(id, request.status, authentication.name, request.note)
        notificationService.notifyStatusChange(updated)
        return ResponseEntity.ok(updated)
    }

    @PatchMapping("/{id}/reassign")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    fun reassignTicket(
        @PathVariable id: String,
        @Valid @RequestBody request: ReassignRequest,
        authentication: Authentication
    ): ResponseEntity<Ticket> {
        val updated = ticketService.reassign(id, request.targetTeam, authentication.name, request.note)
        notificationService.notifyStatusChange(updated)
        return ResponseEntity.ok(updated)
    }

    @GetMapping("/stats")
    fun getStats(): ResponseEntity<TicketStatsResponse> {
        return ResponseEntity.ok(ticketService.getStats())
    }
}
