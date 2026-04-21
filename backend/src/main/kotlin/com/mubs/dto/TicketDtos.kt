package com.mubs.dto

import com.mubs.model.enums.TicketStatus
import jakarta.validation.constraints.NotBlank

data class TicketStatusUpdateRequest(
    @field:NotBlank val status: String,
    val note: String? = null
)

data class TicketFilterParams(
    val status: TicketStatus? = null,
    val eventType: String? = null,
    val assignedTeam: String? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class TicketStatsResponse(
    val total: Long,
    val byStatus: Map<String, Long>,
    val byEventType: Map<String, Long>,
    val byTeam: Map<String, Long>,
    val avgResolutionMinutes: Double?
)

data class FieldworkerDto(
    val username: String,
    val displayName: String?,
    val team: String?
)
