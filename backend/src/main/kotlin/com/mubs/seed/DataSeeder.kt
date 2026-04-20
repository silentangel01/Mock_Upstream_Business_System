package com.mubs.seed

import com.mubs.model.DispatchRule
import com.mubs.model.Ticket
import com.mubs.model.TimelineEntry
import com.mubs.model.User
import com.mubs.model.enums.TicketStatus
import com.mubs.model.enums.UserRole
import com.mubs.repository.DispatchRuleRepository
import com.mubs.repository.TicketRepository
import com.mubs.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class DataSeeder(
    private val userRepository: UserRepository,
    private val dispatchRuleRepository: DispatchRuleRepository,
    private val ticketRepository: TicketRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        seedUsers()
        seedDispatchRules()
        seedHistoricalTickets()
    }

    private fun seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already seeded, skipping")
            return
        }
        val users = listOf(
            User(
                username = "admin",
                passwordHash = passwordEncoder.encode("admin123"),
                role = UserRole.ADMIN,
                displayName = "System Admin",
                email = "admin@mubs.local"
            ),
            User(
                username = "dispatcher_wang",
                passwordHash = passwordEncoder.encode("dispatch123"),
                role = UserRole.DISPATCHER,
                displayName = "Wang Dispatcher",
                email = "wang@mubs.local"
            ),
            User(
                username = "worker_zhang",
                passwordHash = passwordEncoder.encode("worker123"),
                role = UserRole.FIELDWORKER,
                team = "fire_team",
                displayName = "Zhang Fieldworker",
                email = "zhang@mubs.local",
                phone = "13800000001"
            ),
            User(
                username = "worker_li",
                passwordHash = passwordEncoder.encode("worker123"),
                role = UserRole.FIELDWORKER,
                team = "traffic_team",
                displayName = "Li Fieldworker",
                email = "li@mubs.local",
                phone = "13800000002"
            ),
            User(
                username = "worker_chen",
                passwordHash = passwordEncoder.encode("worker123"),
                role = UserRole.FIELDWORKER,
                team = "urban_mgmt_team",
                displayName = "Chen Fieldworker",
                email = "chen@mubs.local",
                phone = "13800000003"
            )
        )
        userRepository.saveAll(users)
        log.info("Seeded {} users", users.size)
    }

    private fun seedDispatchRules() {
        if (dispatchRuleRepository.count() > 0) {
            log.info("Dispatch rules already seeded, skipping")
            return
        }
        val rules = listOf(
            DispatchRule(eventType = "smoke_flame", areaCode = "east_district", targetTeam = "fire_team", priority = 10),
            DispatchRule(eventType = "smoke_flame", areaCode = "*", targetTeam = "fire_team", priority = 1),
            DispatchRule(eventType = "parking_violation", areaCode = "*", targetTeam = "traffic_team", priority = 1),
            DispatchRule(eventType = "common_space_utilization", areaCode = "*", targetTeam = "urban_mgmt_team", priority = 1)
        )
        dispatchRuleRepository.saveAll(rules)
        log.info("Seeded {} dispatch rules", rules.size)
    }

    private fun seedHistoricalTickets() {
        if (ticketRepository.count() > 0) {
            log.info("Tickets already exist, skipping historical seed")
            return
        }
        val now = Instant.now()
        val tickets = listOf(
            ht("seed-001", "smoke_flame", "east_district", "fire_team", TicketStatus.CLOSED,
                now.minus(7, ChronoUnit.DAYS), "Smoke detected near warehouse B", "worker_zhang",
                now.minus(6, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                now.minus(6, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS)),
            ht("seed-002", "parking_violation", "central_district", "traffic_team", TicketStatus.CLOSED,
                now.minus(6, ChronoUnit.DAYS), "Illegal parking on Main Street", "worker_li",
                now.minus(6, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS),
                now.minus(6, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS)),
            ht("seed-003", "common_space_utilization", "west_district", "urban_mgmt_team", TicketStatus.CLOSED,
                now.minus(6, ChronoUnit.DAYS), "High occupancy in West Park", "worker_chen",
                now.minus(5, ChronoUnit.DAYS).plus(5, ChronoUnit.HOURS),
                now.minus(5, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS)),
            ht("seed-004", "smoke_flame", "west_district", "fire_team", TicketStatus.RESOLVED,
                now.minus(5, ChronoUnit.DAYS), "Fire alarm triggered in office complex", "worker_zhang",
                now.minus(4, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS), null),
            ht("seed-005", "parking_violation", "east_district", "traffic_team", TicketStatus.RESOLVED,
                now.minus(5, ChronoUnit.DAYS), "Double parking near school zone", "worker_li",
                now.minus(4, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS), null),
            ht("seed-006", "smoke_flame", "central_district", "fire_team", TicketStatus.CLOSED,
                now.minus(4, ChronoUnit.DAYS), "Kitchen fire in restaurant district", "worker_zhang",
                now.minus(4, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS), now.minus(3, ChronoUnit.DAYS)),
            ht("seed-007", "common_space_utilization", "east_district", "urban_mgmt_team", TicketStatus.CLOSED,
                now.minus(4, ChronoUnit.DAYS), "Overcrowding at east market square", "worker_chen",
                now.minus(3, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS),
                now.minus(3, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS)),
            ht("seed-008", "parking_violation", "west_district", "traffic_team", TicketStatus.IN_PROGRESS,
                now.minus(3, ChronoUnit.DAYS), "Vehicles blocking fire lane", "worker_li", null, null),
            ht("seed-009", "smoke_flame", "east_district", "fire_team", TicketStatus.DISPATCHED,
                now.minus(2, ChronoUnit.DAYS), "Smoke from construction site", null, null, null),
            ht("seed-010", "parking_violation", "central_district", "traffic_team", TicketStatus.DISPATCHED,
                now.minus(2, ChronoUnit.DAYS), "Unauthorized parking in handicap zone", null, null, null),
            ht("seed-011", "common_space_utilization", "central_district", "urban_mgmt_team", TicketStatus.DISPATCHED,
                now.minus(1, ChronoUnit.DAYS), "Crowd gathering at central plaza", null, null, null),
            ht("seed-012", "smoke_flame", "west_district", "fire_team", TicketStatus.PENDING,
                now.minus(1, ChronoUnit.DAYS), "Possible electrical fire in parking garage", null, null, null),
            ht("seed-013", "parking_violation", "east_district", "traffic_team", TicketStatus.RETURNED,
                now.minus(1, ChronoUnit.DAYS), "Truck blocking residential driveway", null, null, null),
            ht("seed-014", "common_space_utilization", "west_district", "urban_mgmt_team", TicketStatus.CLOSED,
                now.minus(3, ChronoUnit.DAYS), "Street vendor overcrowding west entrance", "worker_chen",
                now.minus(2, ChronoUnit.DAYS), now.minus(2, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS)),
            ht("seed-015", "smoke_flame", "central_district", "fire_team", TicketStatus.CLOSED,
                now.minus(5, ChronoUnit.DAYS), "BBQ smoke false alarm at park", "worker_zhang",
                now.minus(5, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS), now.minus(4, ChronoUnit.DAYS))
        )
        ticketRepository.saveAll(tickets)
        log.info("Seeded {} historical tickets", tickets.size)
    }

    private fun ht(
        eventId: String, eventType: String, areaCode: String, team: String,
        status: TicketStatus, createdAt: Instant, description: String,
        assignedUser: String?, resolvedAt: Instant?, closedAt: Instant?
    ): Ticket {
        val timeline = mutableListOf(
            TimelineEntry(action = "CREATED", actor = "system", timestamp = createdAt, note = "Created from HVAS webhook")
        )
        val dispAt = createdAt.plus(5, ChronoUnit.MINUTES)
        if (status != TicketStatus.PENDING) {
            timeline.add(TimelineEntry(action = "AUTO_DISPATCHED", actor = "system", timestamp = dispAt,
                note = "Matched rule: $eventType/* → $team"))
        }
        if (assignedUser != null) {
            timeline.add(TimelineEntry(action = "STATUS_CHANGE", actor = assignedUser,
                timestamp = dispAt.plus(30, ChronoUnit.MINUTES), note = "DISPATCHED → ACCEPTED"))
            timeline.add(TimelineEntry(action = "STATUS_CHANGE", actor = assignedUser,
                timestamp = dispAt.plus(1, ChronoUnit.HOURS), note = "ACCEPTED → IN_PROGRESS"))
        }
        if (resolvedAt != null) {
            timeline.add(TimelineEntry(action = "STATUS_CHANGE", actor = assignedUser ?: "system",
                timestamp = resolvedAt, note = "IN_PROGRESS → RESOLVED. Issue handled."))
        }
        if (closedAt != null) {
            timeline.add(TimelineEntry(action = "STATUS_CHANGE", actor = "admin",
                timestamp = closedAt, note = "RESOLVED → CLOSED"))
        }
        if (status == TicketStatus.RETURNED) {
            timeline.add(TimelineEntry(action = "STATUS_CHANGE", actor = "system",
                timestamp = dispAt.plus(2, ChronoUnit.HOURS), note = "DISPATCHED → RETURNED"))
        }
        return Ticket(
            hvasEventId = eventId, eventType = eventType,
            cameraId = "CAM-SEED-${eventId.takeLast(3)}",
            eventTimestamp = createdAt.epochSecond.toDouble(),
            createdAt = createdAt, confidence = 0.75 + (Math.random() * 0.2),
            description = description, areaCode = areaCode,
            location = "${areaCode.replace("_", " ")} area",
            status = status, assignedTeam = team, assignedUser = assignedUser,
            dispatchedAt = if (status != TicketStatus.PENDING) dispAt else null,
            resolvedAt = resolvedAt, closedAt = closedAt, timeline = timeline
        )
    }
}
