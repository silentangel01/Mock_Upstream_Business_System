package com.mubs.service

import com.mubs.dto.DemoEventRequest
import com.mubs.dto.HvasWebhookPayload
import com.mubs.model.Ticket
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class DemoService(
    private val ticketService: TicketService,
    private val dispatchService: DispatchService,
    private val notificationService: NotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class ScenarioTemplate(
        val eventType: String,
        val areaCode: String,
        val description: String,
        val cameraId: String,
        val confidence: Double
    )

    private val templates = listOf(
        ScenarioTemplate(
            eventType = "smoke_flame",
            areaCode = "east_district",
            description = "Smoke detected near Building A, east district",
            cameraId = "CAM-DEMO-001",
            confidence = 0.92
        ),
        ScenarioTemplate(
            eventType = "parking_violation",
            areaCode = "central_district",
            description = "Illegal parking detected on Main Street",
            cameraId = "CAM-DEMO-002",
            confidence = 0.87
        ),
        ScenarioTemplate(
            eventType = "common_space_utilization",
            areaCode = "west_district",
            description = "High occupancy detected in West Park plaza area",
            cameraId = "CAM-DEMO-003",
            confidence = 0.78
        )
    )

    fun simulateEvent(request: DemoEventRequest?): Ticket {
        val template = if (request?.eventType != null) {
            templates.firstOrNull { it.eventType == request.eventType } ?: templates.random()
        } else {
            templates.random()
        }

        val payload = HvasWebhookPayload(
            eventId = "demo-${UUID.randomUUID()}",
            eventType = request?.eventType ?: template.eventType,
            cameraId = template.cameraId,
            timestamp = Instant.now().epochSecond.toDouble(),
            createdAt = Instant.now().toString(),
            confidence = template.confidence,
            description = request?.description ?: template.description,
            areaCode = request?.areaCode ?: template.areaCode,
            location = "Demo Location"
        )

        val ticket = ticketService.createFromWebhook(payload)
        val dispatched = dispatchService.autoDispatch(ticket)
        notificationService.notifyNewTicket(dispatched)

        log.info("Demo event simulated: ticket={}, type={}", dispatched.id, dispatched.eventType)
        return dispatched
    }
}
