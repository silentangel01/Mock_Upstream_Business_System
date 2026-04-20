package com.mubs.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val first: Boolean,
    val last: Boolean
)

@Serializable
data class TicketStatusUpdateRequest(
    val status: String,
    val note: String? = null
)

@Serializable
data class ReassignRequest(
    val targetTeam: String,
    val note: String? = null
)
