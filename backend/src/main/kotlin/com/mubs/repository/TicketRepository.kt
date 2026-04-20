package com.mubs.repository

import com.mubs.model.Ticket
import com.mubs.model.enums.TicketStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface TicketRepository : MongoRepository<Ticket, String> {
    fun existsByHvasEventId(hvasEventId: String): Boolean
    fun findByHvasEventId(hvasEventId: String): Ticket?
    fun findByStatus(status: TicketStatus, pageable: Pageable): Page<Ticket>
    fun findByAssignedTeam(team: String, pageable: Pageable): Page<Ticket>
    fun findByStatusAndDispatchedAtBefore(
        status: TicketStatus,
        before: java.time.Instant
    ): List<Ticket>
}
