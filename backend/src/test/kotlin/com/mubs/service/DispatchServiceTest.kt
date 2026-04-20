package com.mubs.service

import com.mubs.model.DispatchRule
import com.mubs.model.Ticket
import com.mubs.model.enums.TicketStatus
import com.mubs.repository.DispatchRuleRepository
import com.mubs.repository.TicketRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class DispatchServiceTest {

    @Mock
    lateinit var dispatchRuleRepository: DispatchRuleRepository

    @Mock
    lateinit var ticketRepository: TicketRepository

    lateinit var dispatchService: DispatchService

    @BeforeEach
    fun setUp() {
        dispatchService = DispatchService(dispatchRuleRepository, ticketRepository, 30L)
    }

    @Test
    fun `should dispatch smoke_flame in east_district to fire_team with highest priority rule`() {
        val rules = listOf(
            DispatchRule(eventType = "smoke_flame", areaCode = "east_district", targetTeam = "fire_team", priority = 10),
            DispatchRule(eventType = "smoke_flame", areaCode = "*", targetTeam = "fire_team", priority = 1)
        )
        whenever(dispatchRuleRepository.findByEventTypeAndEnabledTrueOrderByPriorityDesc("smoke_flame"))
            .thenReturn(rules)
        whenever(ticketRepository.save(any<Ticket>())).thenAnswer { it.arguments[0] }

        val ticket = createTestTicket(eventType = "smoke_flame", areaCode = "east_district")
        val result = dispatchService.autoDispatch(ticket)

        assertEquals(TicketStatus.DISPATCHED, result.status)
        assertEquals("fire_team", result.assignedTeam)
        assertNotNull(result.dispatchedAt)
    }

    @Test
    fun `should dispatch smoke_flame in unknown area using wildcard rule`() {
        val rules = listOf(
            DispatchRule(eventType = "smoke_flame", areaCode = "east_district", targetTeam = "fire_team", priority = 10),
            DispatchRule(eventType = "smoke_flame", areaCode = "*", targetTeam = "fire_team", priority = 1)
        )
        whenever(dispatchRuleRepository.findByEventTypeAndEnabledTrueOrderByPriorityDesc("smoke_flame"))
            .thenReturn(rules)
        whenever(ticketRepository.save(any<Ticket>())).thenAnswer { it.arguments[0] }

        val ticket = createTestTicket(eventType = "smoke_flame", areaCode = "west_district")
        val result = dispatchService.autoDispatch(ticket)

        assertEquals(TicketStatus.DISPATCHED, result.status)
        assertEquals("fire_team", result.assignedTeam)
    }

    @Test
    fun `should dispatch parking_violation to traffic_team`() {
        val rules = listOf(
            DispatchRule(eventType = "parking_violation", areaCode = "*", targetTeam = "traffic_team", priority = 1)
        )
        whenever(dispatchRuleRepository.findByEventTypeAndEnabledTrueOrderByPriorityDesc("parking_violation"))
            .thenReturn(rules)
        whenever(ticketRepository.save(any<Ticket>())).thenAnswer { it.arguments[0] }

        val ticket = createTestTicket(eventType = "parking_violation", areaCode = "anywhere")
        val result = dispatchService.autoDispatch(ticket)

        assertEquals("traffic_team", result.assignedTeam)
    }

    @Test
    fun `should leave ticket as PENDING when no rules match`() {
        whenever(dispatchRuleRepository.findByEventTypeAndEnabledTrueOrderByPriorityDesc("unknown_type"))
            .thenReturn(emptyList())
        whenever(ticketRepository.save(any<Ticket>())).thenAnswer { it.arguments[0] }

        val ticket = createTestTicket(eventType = "unknown_type", areaCode = "anywhere")
        val result = dispatchService.autoDispatch(ticket)

        assertEquals(TicketStatus.PENDING, result.status)
        assertNull(result.assignedTeam)
    }

    private fun createTestTicket(eventType: String, areaCode: String) = Ticket(
        id = "test-id",
        hvasEventId = "hvas-123",
        eventType = eventType,
        cameraId = "cam-01",
        eventTimestamp = 1700000000.0,
        confidence = 0.95,
        areaCode = areaCode
    )
}
