package com.mubs.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mubs.dto.HvasWebhookPayload
import com.mubs.service.DispatchService
import com.mubs.service.NotificationService
import com.mubs.service.TicketService
import com.mubs.service.WebhookVerificationService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/v1/hvas")
class HvasWebhookController(
    private val webhookVerificationService: WebhookVerificationService,
    private val ticketService: TicketService,
    private val dispatchService: DispatchService,
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/webhook")
    fun receiveWebhook(@RequestBody rawBody: ByteArray, request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        // Verify HMAC signature
        val signature = request.getHeader("X-HVAS-Signature")
        if (!webhookVerificationService.verifySignature(rawBody, signature)) {
            log.warn("Webhook signature verification failed")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid signature"))
        }

        // Parse payload
        val payload = try {
            objectMapper.readValue(rawBody, HvasWebhookPayload::class.java)
        } catch (e: Exception) {
            log.error("Failed to parse webhook payload: {}", e.message)
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Invalid payload"))
        }

        // Dedup check
        if (ticketService.isDuplicate(payload.eventId)) {
            log.info("Duplicate event ignored: {}", payload.eventId)
            return ResponseEntity.ok(mapOf("status" to "duplicate", "event_id" to payload.eventId))
        }

        // Create ticket
        val ticket = ticketService.createFromWebhook(payload)

        // Auto-dispatch
        val dispatched = dispatchService.autoDispatch(ticket)

        // Notify via WebSocket
        notificationService.notifyNewTicket(dispatched)

        log.info("Webhook processed: ticket={}, team={}, user={}", dispatched.id, dispatched.assignedTeam, dispatched.assignedUser)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mapOf(
                "status" to "created",
                "ticket_id" to (dispatched.id ?: ""),
                "assigned_team" to (dispatched.assignedTeam ?: "unassigned"),
                "assigned_user" to (dispatched.assignedUser ?: "unassigned")
            ))
    }
}
