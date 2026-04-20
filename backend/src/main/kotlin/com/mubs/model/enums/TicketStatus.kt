package com.mubs.model.enums

enum class TicketStatus {
    PENDING,
    DISPATCHED,
    ACCEPTED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    RETURNED;

    companion object {
        private val allowedTransitions = mapOf(
            PENDING to setOf(DISPATCHED, CLOSED),
            DISPATCHED to setOf(ACCEPTED, RETURNED),
            ACCEPTED to setOf(IN_PROGRESS, RETURNED),
            IN_PROGRESS to setOf(RESOLVED, RETURNED),
            RESOLVED to setOf(CLOSED),
            RETURNED to setOf(DISPATCHED, CLOSED),
            CLOSED to emptySet()
        )

        fun canTransition(from: TicketStatus, to: TicketStatus): Boolean {
            return allowedTransitions[from]?.contains(to) ?: false
        }
    }
}
