package com.mubs.service

import com.mubs.model.enums.TicketStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TicketStatusTransitionTest {

    @Test
    fun `PENDING can transition to DISPATCHED`() {
        assertTrue(TicketStatus.canTransition(TicketStatus.PENDING, TicketStatus.DISPATCHED))
    }

    @Test
    fun `PENDING can transition to CLOSED`() {
        assertTrue(TicketStatus.canTransition(TicketStatus.PENDING, TicketStatus.CLOSED))
    }

    @Test
    fun `PENDING cannot transition to RESOLVED`() {
        assertFalse(TicketStatus.canTransition(TicketStatus.PENDING, TicketStatus.RESOLVED))
    }

    @Test
    fun `DISPATCHED can transition to ACCEPTED or RETURNED`() {
        assertTrue(TicketStatus.canTransition(TicketStatus.DISPATCHED, TicketStatus.ACCEPTED))
        assertTrue(TicketStatus.canTransition(TicketStatus.DISPATCHED, TicketStatus.RETURNED))
    }

    @Test
    fun `IN_PROGRESS can transition to RESOLVED or RETURNED`() {
        assertTrue(TicketStatus.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED))
        assertTrue(TicketStatus.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.RETURNED))
    }

    @Test
    fun `RESOLVED can only transition to CLOSED`() {
        assertTrue(TicketStatus.canTransition(TicketStatus.RESOLVED, TicketStatus.CLOSED))
        assertFalse(TicketStatus.canTransition(TicketStatus.RESOLVED, TicketStatus.DISPATCHED))
    }

    @Test
    fun `CLOSED is terminal - cannot transition`() {
        assertFalse(TicketStatus.canTransition(TicketStatus.CLOSED, TicketStatus.PENDING))
        assertFalse(TicketStatus.canTransition(TicketStatus.CLOSED, TicketStatus.DISPATCHED))
    }

    @Test
    fun `RETURNED can transition to DISPATCHED or CLOSED`() {
        assertTrue(TicketStatus.canTransition(TicketStatus.RETURNED, TicketStatus.DISPATCHED))
        assertTrue(TicketStatus.canTransition(TicketStatus.RETURNED, TicketStatus.CLOSED))
    }

    @Test
    fun `full happy path PENDING to CLOSED`() {
        assertTrue(TicketStatus.canTransition(TicketStatus.PENDING, TicketStatus.DISPATCHED))
        assertTrue(TicketStatus.canTransition(TicketStatus.DISPATCHED, TicketStatus.ACCEPTED))
        assertTrue(TicketStatus.canTransition(TicketStatus.ACCEPTED, TicketStatus.IN_PROGRESS))
        assertTrue(TicketStatus.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED))
        assertTrue(TicketStatus.canTransition(TicketStatus.RESOLVED, TicketStatus.CLOSED))
    }
}
