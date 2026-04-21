package com.mubs.service

import com.mubs.model.DispatchRule
import com.mubs.model.RoundRobinCounter
import com.mubs.model.Ticket
import com.mubs.model.User
import com.mubs.model.enums.TicketStatus
import com.mubs.model.enums.UserRole
import com.mubs.repository.DispatchRuleRepository
import com.mubs.repository.RoundRobinCounterRepository
import com.mubs.repository.TicketRepository
import com.mubs.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class DispatchServiceTest {

    @Mock lateinit var dispatchRuleRepository: DispatchRuleRepository
    @Mock lateinit var ticketRepository: TicketRepository
    @Mock lateinit var notificationService: NotificationService
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var roundRobinCounterRepository: RoundRobinCounterRepository

    lateinit var dispatchService: DispatchService

    @BeforeEach
    fun setUp() {
        dispatchService = DispatchService(
            dispatchRuleRepository, ticketRepository, notificationService,
            userRepository, roundRobinCounterRepository, 30L
        )
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
        whenever(userRepository.findByTeamAndRoleAndEnabledTrue("fire_team", UserRole.FIELDWORKER))
            .thenReturn(emptyList())

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
        whenever(userRepository.findByTeamAndRoleAndEnabledTrue("fire_team", UserRole.FIELDWORKER))
            .thenReturn(emptyList())

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
        whenever(userRepository.findByTeamAndRoleAndEnabledTrue("traffic_team", UserRole.FIELDWORKER))
            .thenReturn(emptyList())

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

    @Test
    fun `should round-robin assign to fieldworkers within team`() {
        val rules = listOf(
            DispatchRule(eventType = "smoke_flame", areaCode = "*", targetTeam = "fire_team", priority = 1)
        )
        val workers = listOf(
            User(username = "worker_a", passwordHash = "", role = UserRole.FIELDWORKER, team = "fire_team"),
            User(username = "worker_b", passwordHash = "", role = UserRole.FIELDWORKER, team = "fire_team"),
            User(username = "worker_c", passwordHash = "", role = UserRole.FIELDWORKER, team = "fire_team")
        )

        whenever(dispatchRuleRepository.findByEventTypeAndEnabledTrueOrderByPriorityDesc("smoke_flame"))
            .thenReturn(rules)
        whenever(ticketRepository.save(any<Ticket>())).thenAnswer { it.arguments[0] }
        whenever(userRepository.findByTeamAndRoleAndEnabledTrue("fire_team", UserRole.FIELDWORKER))
            .thenReturn(workers)
        whenever(roundRobinCounterRepository.findById("fire_team"))
            .thenReturn(Optional.of(RoundRobinCounter(team = "fire_team", lastIndex = 0)))
        whenever(roundRobinCounterRepository.save(any<RoundRobinCounter>())).thenAnswer { it.arguments[0] }

        // First dispatch: lastIndex=0, next=(0+1)%3=1 → worker_b
        val ticket1 = createTestTicket(eventType = "smoke_flame", areaCode = "anywhere")
        val result1 = dispatchService.autoDispatch(ticket1)

        assertEquals("fire_team", result1.assignedTeam)
        assertEquals("worker_b", result1.assignedUser)

        // Update mock for second call: lastIndex now 1
        whenever(roundRobinCounterRepository.findById("fire_team"))
            .thenReturn(Optional.of(RoundRobinCounter(team = "fire_team", lastIndex = 1)))

        // Second dispatch: lastIndex=1, next=(1+1)%3=2 → worker_c
        val ticket2 = createTestTicket(eventType = "smoke_flame", areaCode = "anywhere")
        val result2 = dispatchService.autoDispatch(ticket2)

        assertEquals("worker_c", result2.assignedUser)

        // Update mock for third call: lastIndex now 2
        whenever(roundRobinCounterRepository.findById("fire_team"))
            .thenReturn(Optional.of(RoundRobinCounter(team = "fire_team", lastIndex = 2)))

        // Third dispatch: lastIndex=2, next=(2+1)%3=0 → worker_a (wraps around)
        val ticket3 = createTestTicket(eventType = "smoke_flame", areaCode = "anywhere")
        val result3 = dispatchService.autoDispatch(ticket3)

        assertEquals("worker_a", result3.assignedUser)
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
