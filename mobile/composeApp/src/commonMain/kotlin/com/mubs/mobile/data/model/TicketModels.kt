package com.mubs.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TicketStatus {
    PENDING,
    DISPATCHED,
    ACCEPTED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    RETURNED
}

@Serializable
data class TimelineEntry(
    val action: String,
    val actor: String,
    val timestamp: String,
    val note: String? = null
)

@Serializable
data class Ticket(
    val id: String? = null,
    val hvasEventId: String,
    val eventType: String,
    val cameraId: String,
    val eventTimestamp: Double,
    val createdAt: String? = null,
    val confidence: Double,
    val imageUrl: String? = null,
    val description: String? = null,
    val objectCount: Int? = null,
    val latLng: String? = null,
    val location: String? = null,
    val areaCode: String? = null,
    val group: String? = null,
    val status: TicketStatus = TicketStatus.PENDING,
    val assignedTeam: String? = null,
    val assignedUser: String? = null,
    val dispatchedAt: String? = null,
    val resolvedAt: String? = null,
    val closedAt: String? = null,
    val timeline: List<TimelineEntry> = emptyList(),
    val handlePhotos: List<String> = emptyList()
)
