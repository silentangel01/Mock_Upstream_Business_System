package com.mubs.model

import com.mubs.model.enums.TicketStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "tickets")
data class Ticket(
    @Id val id: String? = null,
    @Indexed val hvasEventId: String,
    val eventType: String,
    val cameraId: String,
    val eventTimestamp: Double,
    val createdAt: Instant = Instant.now(),
    val confidence: Double,
    val imageUrl: String? = null,
    val description: String? = null,
    val objectCount: Int? = null,
    val latLng: String? = null,
    val location: String? = null,
    val areaCode: String? = null,
    val group: String? = null,
    @Indexed var status: TicketStatus = TicketStatus.PENDING,
    @Indexed var assignedTeam: String? = null,
    var assignedUser: String? = null,
    var dispatchedAt: Instant? = null,
    var resolvedAt: Instant? = null,
    var closedAt: Instant? = null,
    val timeline: MutableList<TimelineEntry> = mutableListOf(),
    var handlePhotos: MutableList<String> = mutableListOf()
)

data class TimelineEntry(
    val action: String,
    val actor: String,
    val timestamp: Instant = Instant.now(),
    val note: String? = null
)
