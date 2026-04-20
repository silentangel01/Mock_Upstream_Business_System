package com.mubs.service

import com.mubs.model.Ticket
import com.mubs.model.TimelineEntry
import com.mubs.model.enums.TicketStatus
import com.mubs.repository.DispatchRuleRepository
import com.mubs.repository.TicketRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DispatchService(
    private val dispatchRuleRepository: DispatchRuleRepository,
    private val ticketRepository: TicketRepository,
    @Value("\${mubs.dispatch.timeout-minutes}") private val timeoutMinutes: Long
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun autoDispatch(ticket: Ticket): Ticket {
        val rules = dispatchRuleRepository
            .findByEventTypeAndEnabledTrueOrderByPriorityDesc(ticket.eventType)

        val matchedRule = rules.firstOrNull { rule ->
            rule.areaCode == "*" || rule.areaCode == ticket.areaCode
        }

        if (matchedRule != null) {
            ticket.assignedTeam = matchedRule.targetTeam
            ticket.status = TicketStatus.DISPATCHED
            ticket.dispatchedAt = Instant.now()
            ticket.timeline.add(
                TimelineEntry(
                    action = "AUTO_DISPATCHED",
                    actor = "system",
                    note = "Matched rule: ${matchedRule.eventType}/${matchedRule.areaCode} → ${matchedRule.targetTeam}"
                )
            )
            log.info("Auto-dispatched ticket {} to team {}", ticket.id, matchedRule.targetTeam)
        } else {
            log.warn("No dispatch rule matched for ticket {} (eventType={}, areaCode={})",
                ticket.id, ticket.eventType, ticket.areaCode)
        }

        return ticketRepository.save(ticket)
    }

    @Scheduled(fixedDelayString = "\${mubs.dispatch.timeout-minutes:30}000")
    fun checkTimeouts() {
        val cutoff = Instant.now().minusSeconds(timeoutMinutes * 60)
        val timedOut = ticketRepository.findByStatusAndDispatchedAtBefore(
            TicketStatus.DISPATCHED, cutoff
        )
        for (ticket in timedOut) {
            ticket.status = TicketStatus.RETURNED
            ticket.timeline.add(
                TimelineEntry(
                    action = "TIMEOUT_RETURNED",
                    actor = "system",
                    note = "No acceptance within $timeoutMinutes minutes"
                )
            )
            ticketRepository.save(ticket)
            log.info("Ticket {} returned due to dispatch timeout", ticket.id)
        }
    }
}
