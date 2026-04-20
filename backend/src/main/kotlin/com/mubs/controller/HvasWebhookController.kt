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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.ContentCachingRequestWrapper
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
    fun receiveWebhook(request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        val cachedRequest = request as? ContentCachingRequestWrapper
            ?: return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Request not cached"))

        // Read body — must read input stream first to populate cache
        val body = cachedRequest.inputStream.readAllBytes()
        val rawBody = if (body.isNotEmpty()) body else cachedRequest.contentAsByteArray

        // Verify HMAC signature
        val signature = cachedRequest.getHeader("X-HVAS-Signature")
        if (!webhookVerificationService.verifySignature(rawBody, signature)) {
            log.warn("Webhook signature verification failed")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid signature"))
        }

        // Parse payload
        val payload = try {
            objectMapper.readValue(rawBody, HvasWebhookPayload::class.java)
        } catch (e: Exception) {
            log.error("Failed to parse webhook payload", e)
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

        // Notify
        notificationService.notifyNewTicket(dispatched)

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mapOf(
                "status" to "created",
                "ticket_id" to (dispatched.id ?: ""),
                "assigned_team" to (dispatched.assignedTeam ?: "unassigned")
            ))
    }
}
