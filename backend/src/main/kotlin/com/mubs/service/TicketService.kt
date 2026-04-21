package com.mubs.service

import com.mubs.dto.FieldworkerDto
import com.mubs.dto.HvasWebhookPayload
import com.mubs.dto.TicketFilterParams
import com.mubs.dto.TicketStatsResponse
import com.mubs.model.Ticket
import com.mubs.model.TimelineEntry
import com.mubs.model.enums.TicketStatus
import com.mubs.model.enums.UserRole
import com.mubs.repository.TicketRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TicketService(
    private val ticketRepository: TicketRepository,
    private val mongoTemplate: MongoTemplate,
    private val hvasApiClient: HvasApiClient,
    private val userRepository: com.mubs.repository.UserRepository,
    private val notificationService: NotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun createFromWebhook(payload: HvasWebhookPayload): Ticket {
        val ticket = Ticket(
            hvasEventId = payload.eventId,
            eventType = payload.eventType,
            cameraId = payload.cameraId,
            eventTimestamp = payload.timestamp,
            confidence = payload.confidence,
            imageUrl = payload.imageUrl,
            description = payload.description,
            objectCount = payload.objectCount,
            latLng = payload.latLng,
            location = payload.location,
            areaCode = payload.areaCode ?: "",
            group = payload.group ?: "",
            timeline = mutableListOf(
                TimelineEntry(action = "CREATED", actor = "system", note = "Created from HVAS webhook")
            )
        )
        val saved = ticketRepository.save(ticket)
        log.info("Created ticket {} from HVAS event {}", saved.id, payload.eventId)
        return saved
    }

    fun isDuplicate(eventId: String): Boolean = ticketRepository.existsByHvasEventId(eventId)

    fun findById(id: String): Ticket? = ticketRepository.findById(id).orElse(null)

    fun save(ticket: Ticket): Ticket = ticketRepository.save(ticket)

    fun findAll(params: TicketFilterParams, authentication: Authentication): Page<Ticket> {
        val pageable = PageRequest.of(params.page, params.size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val query = Query().with(pageable)

        params.status?.let { query.addCriteria(Criteria.where("status").`is`(it)) }
        params.eventType?.let { query.addCriteria(Criteria.where("eventType").`is`(it)) }
        params.assignedTeam?.let { query.addCriteria(Criteria.where("assignedTeam").`is`(it)) }

        // FIELDWORKER can only see tickets assigned to them personally
        val isFieldworker = authentication.authorities.any { it.authority == "ROLE_FIELDWORKER" }
        if (isFieldworker) {
            query.addCriteria(Criteria.where("assignedUser").`is`(authentication.name))
        }

        val results = mongoTemplate.find(query, Ticket::class.java)
        return PageableExecutionUtils.getPage(results, pageable) {
            mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Ticket::class.java)
        }
    }

    fun updateStatus(id: String, newStatusStr: String, actor: String, note: String?): Ticket {
        val ticket = ticketRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Ticket not found: $id") }
        val newStatus = TicketStatus.valueOf(newStatusStr.uppercase())

        if (!TicketStatus.canTransition(ticket.status, newStatus)) {
            throw IllegalStateException(
                "Invalid transition from ${ticket.status} to $newStatus"
            )
        }

        val oldStatus = ticket.status
        ticket.status = newStatus
        ticket.timeline.add(
            TimelineEntry(action = "STATUS_CHANGE", actor = actor, note = "$oldStatus → $newStatus. ${note ?: ""}")
        )

        when (newStatus) {
            TicketStatus.DISPATCHED -> ticket.dispatchedAt = Instant.now()
            TicketStatus.RESOLVED -> ticket.resolvedAt = Instant.now()
            TicketStatus.CLOSED -> ticket.closedAt = Instant.now()
            else -> {}
        }

        val saved = ticketRepository.save(ticket)

        // Callback to HVAS
        val hvasStatus = when (newStatus) {
            TicketStatus.DISPATCHED -> "dispatched"
            TicketStatus.ACCEPTED, TicketStatus.IN_PROGRESS -> "processing"
            TicketStatus.RESOLVED -> "resolved"
            else -> null
        }
        if (hvasStatus != null) {
            hvasApiClient.updateEventStatus(
                hvasEventId = saved.hvasEventId,
                status = hvasStatus,
                handledBy = if (newStatus == TicketStatus.RESOLVED) actor else null,
                handleNote = if (newStatus == TicketStatus.RESOLVED) note else null,
                handleImageUrl = if (newStatus == TicketStatus.RESOLVED) saved.handlePhotos.firstOrNull() else null
            )
        }

        return saved
    }

    fun reassign(id: String, targetUser: String, actor: String, note: String?): Ticket {
        val ticket = ticketRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Ticket not found: $id") }

        val allowedStatuses = setOf(TicketStatus.DISPATCHED, TicketStatus.RETURNED, TicketStatus.PENDING)
        if (ticket.status !in allowedStatuses) {
            throw IllegalStateException("Cannot reassign ticket in status ${ticket.status}")
        }

        val target = userRepository.findByUsername(targetUser)
            ?: throw IllegalArgumentException("User not found: $targetUser")

        val previousUser = ticket.assignedUser
        val previousTeam = ticket.assignedTeam
        ticket.assignedUser = target.username
        ticket.assignedTeam = target.team
        ticket.status = TicketStatus.DISPATCHED
        ticket.dispatchedAt = Instant.now()
        ticket.timeline.add(
            TimelineEntry(
                action = "REASSIGNED",
                actor = actor,
                note = "Reassigned from ${previousUser ?: previousTeam} to ${target.username}. ${note ?: ""}"
            )
        )
        log.info("Ticket {} reassigned to user {} (team {}) by {}", id, target.username, target.team, actor)
        val saved = ticketRepository.save(ticket)
        notificationService.notifyNewTicket(saved)
        return saved
    }

    fun listFieldworkers(team: String?): List<FieldworkerDto> {
        val workers = if (team != null) {
            userRepository.findByTeamAndRoleAndEnabledTrue(team, UserRole.FIELDWORKER)
        } else {
            userRepository.findByRoleAndEnabledTrue(UserRole.FIELDWORKER)
        }
        return workers.map { FieldworkerDto(it.username, it.displayName, it.team) }
    }

    fun getStats(): TicketStatsResponse {
        val total = ticketRepository.count()

        val byStatus = mongoTemplate.aggregate(
            Aggregation.newAggregation(
                Aggregation.group("status").count().`as`("count")
            ),
            "tickets", Map::class.java
        ).mappedResults.associate { (it["_id"] as String) to (it["count"] as Number).toLong() }

        val byEventType = mongoTemplate.aggregate(
            Aggregation.newAggregation(
                Aggregation.group("eventType").count().`as`("count")
            ),
            "tickets", Map::class.java
        ).mappedResults.associate { (it["_id"] as String) to (it["count"] as Number).toLong() }

        val byTeam = mongoTemplate.aggregate(
            Aggregation.newAggregation(
                Aggregation.match(Criteria.where("assignedTeam").ne(null)),
                Aggregation.group("assignedTeam").count().`as`("count")
            ),
            "tickets", Map::class.java
        ).mappedResults.associate { (it["_id"] as String) to (it["count"] as Number).toLong() }

        val avgResolution = mongoTemplate.aggregate(
            Aggregation.newAggregation(
                Aggregation.match(Criteria.where("resolvedAt").ne(null).and("createdAt").ne(null)),
                Aggregation.project()
                    .andExpression("resolvedAt - createdAt").`as`("resolutionMs"),
                Aggregation.group().avg("resolutionMs").`as`("avg")
            ),
            "tickets", Map::class.java
        ).mappedResults.firstOrNull()?.let {
            val avgMs = (it["avg"] as? Number)?.toDouble()
            avgMs?.let { ms -> ms / 60000.0 }
        }

        return TicketStatsResponse(
            total = total,
            byStatus = byStatus,
            byEventType = byEventType,
            byTeam = byTeam,
            avgResolutionMinutes = avgResolution
        )
    }
}
