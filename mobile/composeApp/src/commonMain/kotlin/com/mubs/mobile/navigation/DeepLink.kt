package com.mubs.mobile.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DeepLink {
    private val _pendingTicketId = MutableStateFlow<String?>(null)
    val pendingTicketId: StateFlow<String?> = _pendingTicketId

    fun offer(ticketId: String) {
        _pendingTicketId.value = ticketId
    }

    fun consume(): String? {
        val id = _pendingTicketId.value
        _pendingTicketId.value = null
        return id
    }
}
