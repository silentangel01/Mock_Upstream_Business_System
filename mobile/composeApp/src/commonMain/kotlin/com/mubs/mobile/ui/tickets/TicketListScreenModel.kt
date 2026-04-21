package com.mubs.mobile.ui.tickets

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mubs.mobile.data.model.Ticket
import com.mubs.mobile.data.model.TicketStatus
import com.mubs.mobile.data.repository.TicketRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TicketListUiState(
    val tickets: List<Ticket> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedStatus: TicketStatus? = null,
    val currentPage: Int = 0,
    val hasMore: Boolean = true
)

class TicketListScreenModel(
    private val ticketRepository: TicketRepository
) : ScreenModel {

    private val _state = MutableStateFlow(TicketListUiState())
    val state: StateFlow<TicketListUiState> = _state.asStateFlow()

    init {
        loadTickets()
        startPolling()
    }

    private fun startPolling() {
        screenModelScope.launch {
            while (isActive) {
                delay(30_000L)
                // Silent refresh — don't show loading indicator
                ticketRepository.listTickets(
                    status = _state.value.selectedStatus,
                    page = 0
                ).onSuccess { page ->
                    _state.value = _state.value.copy(
                        tickets = page.content,
                        hasMore = !page.last
                    )
                }
            }
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true, currentPage = 0)
        loadTickets(reset = true)
    }

    fun filterByStatus(status: TicketStatus?) {
        _state.value = _state.value.copy(selectedStatus = status, currentPage = 0)
        loadTickets(reset = true)
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoading || !current.hasMore) return
        _state.value = current.copy(currentPage = current.currentPage + 1)
        loadTickets(reset = false)
    }

    private fun loadTickets(reset: Boolean = true) {
        val current = _state.value
        screenModelScope.launch {
            _state.value = current.copy(isLoading = true, error = null)
            ticketRepository.listTickets(
                status = current.selectedStatus,
                page = if (reset) 0 else current.currentPage
            ).onSuccess { page ->
                val tickets = if (reset) {
                    page.content
                } else {
                    val existingIds = current.tickets.mapNotNull { it.id }.toSet()
                    current.tickets + page.content.filter { it.id !in existingIds }
                }
                _state.value = _state.value.copy(
                    tickets = tickets,
                    isLoading = false,
                    isRefreshing = false,
                    hasMore = !page.last
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Load failed"
                )
            }
        }
    }
}
