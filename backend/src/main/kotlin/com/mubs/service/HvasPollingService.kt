package com.mubs.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mubs.dto.HvasWebhookPayload
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
@ConditionalOnProperty(name = ["mubs.hvas.polling.enabled"], havingValue = "true")
class HvasPollingService(
    private val ticketService: TicketService,
    private val dispatchService: DispatchService,
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper,
    @Value("\${mubs.hvas.base-url}") private val hvasBaseUrl: String,
    @Value("\${mubs.hvas.api-key:}") private val apiKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create()
    private var lastPollTimestamp: Double = System.currentTimeMillis() / 1000.0

    @Scheduled(fixedDelayString = "\${mubs.hvas.polling.interval-ms:30000}")
    fun pollEvents() {
        try {
            val url = "$hvasBaseUrl/api/events?since=${lastPollTimestamp}&limit=50"
            val builder = restClient.get().uri(url)
            if (apiKey.isNotBlank()) {
                builder.header("X-API-Key", apiKey)
            }
            val response = builder.retrieve().body(String::class.java)
            if (response.isNullOrBlank()) return

            val events: List<HvasWebhookPayload> = objectMapper.readValue(
                response,
                objectMapper.typeFactory.constructCollectionType(List::class.java, HvasWebhookPayload::class.java)
            )

            for (event in events) {
                if (!ticketService.isDuplicate(event.eventId)) {
                    val ticket = ticketService.createFromWebhook(event)
                    val dispatched = dispatchService.autoDispatch(ticket)
                    notificationService.notifyNewTicket(dispatched)
                }
            }

            if (events.isNotEmpty()) {
                lastPollTimestamp = events.maxOf { e -> e.timestamp }
                log.info("Polled {} new events from HVAS", events.size)
            }
        } catch (e: Exception) {
            log.error("HVAS polling failed", e)
        }
    }
}
