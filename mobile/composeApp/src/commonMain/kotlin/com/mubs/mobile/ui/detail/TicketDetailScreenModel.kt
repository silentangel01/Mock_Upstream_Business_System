package com.mubs.mobile.ui.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mubs.mobile.data.model.Fieldworker
import com.mubs.mobile.data.model.Ticket
import com.mubs.mobile.data.repository.AuthRepository
import com.mubs.mobile.data.repository.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val ticket: Ticket? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val actionInProgress: Boolean = false,
    val role: String? = null,
    val showNoteDialog: Boolean = false,
    val pendingAction: String? = null,
    val showReassignSheet: Boolean = false,
    val fieldworkers: List<Fieldworker> = emptyList()
)

class TicketDetailScreenModel(
    private val ticketId: String,
    private val ticketRepository: TicketRepository,
    private val authRepository: AuthRepository
) : ScreenModel {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        loadTicket()
        screenModelScope.launch {
            _state.value = _state.value.copy(role = authRepository.getRole())
        }
    }

    fun loadTicket() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            ticketRepository.getTicket(ticketId)
                .onSuccess { ticket ->
                    _state.value = _state.value.copy(ticket = ticket, isLoading = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Load failed"
                    )
                }
        }
    }

    fun requestStatusUpdate(status: String) {
        _state.value = _state.value.copy(
            showNoteDialog = true,
            pendingAction = status
        )
    }

    fun confirmStatusUpdate(note: String?) {
        val action = _state.value.pendingAction ?: return
        _state.value = _state.value.copy(showNoteDialog = false, pendingAction = null)
        screenModelScope.launch {
            _state.value = _state.value.copy(actionInProgress = true)
            ticketRepository.updateStatus(ticketId, action, note)
                .onSuccess { ticket ->
                    _state.value = _state.value.copy(ticket = ticket, actionInProgress = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        actionInProgress = false,
                        error = e.message ?: "Operation failed"
                    )
                }
        }
    }

    fun dismissDialog() {
        _state.value = _state.value.copy(
            showNoteDialog = false,
            pendingAction = null,
            showReassignSheet = false
        )
    }

    fun showReassign() {
        _state.value = _state.value.copy(showReassignSheet = true)
        screenModelScope.launch {
            ticketRepository.listFieldworkers()
                .onSuccess { workers ->
                    _state.value = _state.value.copy(fieldworkers = workers)
                }
        }
    }

    fun confirmReassign(targetUser: String, note: String?) {
        _state.value = _state.value.copy(showReassignSheet = false)
        screenModelScope.launch {
            _state.value = _state.value.copy(actionInProgress = true)
            ticketRepository.reassign(ticketId, targetUser, note)
                .onSuccess { ticket ->
                    _state.value = _state.value.copy(ticket = ticket, actionInProgress = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        actionInProgress = false,
                        error = e.message ?: "Reassign failed"
                    )
                }
        }
    }

    fun uploadPhoto(fileName: String, bytes: ByteArray) {
        screenModelScope.launch {
            _state.value = _state.value.copy(actionInProgress = true)
            ticketRepository.uploadPhoto(ticketId, fileName, bytes)
                .onSuccess { loadTicket() }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        actionInProgress = false,
                        error = e.message ?: "Upload failed"
                    )
                }
        }
    }
}
