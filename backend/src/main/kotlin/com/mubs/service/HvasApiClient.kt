package com.mubs.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class HvasApiClient(
    @Value("\${mubs.hvas.base-url}") private val hvasBaseUrl: String,
    @Value("\${mubs.hvas.api-key:}") private val apiKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create()

    @Async
    fun updateEventStatus(hvasEventId: String, status: String, handledBy: String? = null, handleNote: String? = null, handleImageUrl: String? = null) {
        // Skip callback for demo-simulated events (not real HVAS events)
        if (hvasEventId.startsWith("demo-")) {
            log.debug("Skipping HVAS callback for demo event {}", hvasEventId)
            return
        }
        try {
            val body = mutableMapOf<String, Any?>("status" to status)
            handledBy?.let { body["handled_by"] = it }
            handleNote?.let { body["handle_note"] = it }
            handleImageUrl?.let { body["handle_image_url"] = it }

            val builder = restClient.patch()
                .uri("$hvasBaseUrl/api/events/$hvasEventId/status")
                .header("Content-Type", "application/json")
            if (apiKey.isNotBlank()) {
                builder.header("X-API-Key", apiKey)
            }
            builder.body(body).retrieve().toBodilessEntity()
            log.info("HVAS status updated for event {}: {}", hvasEventId, status)
        } catch (e: Exception) {
            log.warn("Failed to update HVAS event {} status to {}: {}", hvasEventId, status, e.message)
        }
    }
}
