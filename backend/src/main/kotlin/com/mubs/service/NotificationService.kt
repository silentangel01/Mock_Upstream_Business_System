package com.mubs.service

import com.mubs.model.NotificationLog
import com.mubs.model.Ticket
import com.mubs.model.enums.NotificationChannel
import com.mubs.model.enums.UserRole
import com.mubs.repository.NotificationLogRepository
import com.mubs.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val notificationLogRepository: NotificationLogRepository,
    private val messagingTemplate: SimpMessagingTemplate,
    private val mailSender: JavaMailSender?,
    private val smsService: SmsService,
    private val userRepository: UserRepository,
    @Value("\${mubs.notification.email.enabled:false}") private val emailEnabled: Boolean,
    @Value("\${mubs.notification.email.from:noreply@yourdomain.com}") private val emailFrom: String,
    @Value("\${mubs.h5.base-url:http://localhost:5173}") private val h5BaseUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun notifyNewTicket(ticket: Ticket) {
        val message = "New ticket [${ticket.id}] — ${ticket.eventType}: ${ticket.description ?: "No description"}"

        // 1. WebSocket broadcast
        sendWebSocket(ticket, message)

        val team = ticket.assignedTeam ?: return
        val teamUsers = userRepository.findByTeam(team)

        // 2. Email to team members with email addresses
        if (emailEnabled) {
            val subject = "New MUBS Ticket Assigned: ${ticket.eventType}"
            val body = "$message\n\nView ticket: $h5BaseUrl/tickets/${ticket.id}"
            teamUsers.filter { !it.email.isNullOrBlank() }.forEach { user ->
                sendEmail(ticket, user.email!!, subject, body)
            }
        }

        // 3. SMS to fieldworkers with phone numbers
        if (smsService.isEnabled()) {
            teamUsers
                .filter { it.role == UserRole.FIELDWORKER && !it.phone.isNullOrBlank() }
                .forEach { user ->
                    val params = mapOf(
                        "ticketId" to (ticket.id ?: ""),
                        "eventType" to ticket.eventType,
                        "description" to (ticket.description?.take(20) ?: "")
                    )
                    val success = smsService.sendSms(
                        user.phone!!, smsService.getDispatchTemplate(), params
                    )
                    logNotification(ticket, NotificationChannel.SMS, user.phone!!, message, success)
                }
        }
    }

    fun notifyStatusChange(ticket: Ticket) {
        val message = "Ticket [${ticket.id}] status changed to ${ticket.status}"
        sendWebSocket(ticket, message)
    }

    fun notifyDispatchTimeout(ticket: Ticket) {
        val message = "Ticket [${ticket.id}] dispatch timeout — ${ticket.eventType}: ${ticket.description ?: ""}"

        // 1. WebSocket broadcast
        sendWebSocket(ticket, message)

        // 2. Email admins
        if (emailEnabled) {
            val admins = userRepository.findAll().filter { it.role == UserRole.ADMIN && !it.email.isNullOrBlank() }
            val subject = "MUBS Dispatch Timeout: ${ticket.eventType}"
            val body = "$message\n\nView ticket: $h5BaseUrl/tickets/${ticket.id}"
            admins.forEach { admin ->
                sendEmail(ticket, admin.email!!, subject, body)
            }
        }

        // 3. SMS to team lead (first fieldworker in team)
        if (smsService.isEnabled() && ticket.assignedTeam != null) {
            val teamUsers = userRepository.findByTeam(ticket.assignedTeam!!)
            teamUsers
                .filter { it.role == UserRole.FIELDWORKER && !it.phone.isNullOrBlank() }
                .firstOrNull()
                ?.let { lead ->
                    val params = mapOf(
                        "ticketId" to (ticket.id ?: ""),
                        "eventType" to ticket.eventType
                    )
                    val success = smsService.sendSms(
                        lead.phone!!, smsService.getTimeoutTemplate(), params
                    )
                    logNotification(ticket, NotificationChannel.SMS, lead.phone!!, message, success)
                }
        }
    }

    private fun sendWebSocket(ticket: Ticket, message: String) {
        try {
            val payload = mapOf(
                "ticket" to ticket,
                "message" to message,
                "actionUrl" to "$h5BaseUrl/tickets/${ticket.id}"
            )
            messagingTemplate.convertAndSend("/topic/tickets/all", payload)
            if (ticket.assignedTeam != null) {
                messagingTemplate.convertAndSend("/topic/tickets/${ticket.assignedTeam}", payload)
            }
            logNotification(ticket, NotificationChannel.WEBSOCKET, "/topic/tickets/all", message, true)
        } catch (e: Exception) {
            log.error("WebSocket notification failed for ticket {}", ticket.id, e)
            logNotification(ticket, NotificationChannel.WEBSOCKET, "/topic/tickets/all", message, false, e.message)
        }
    }

    private fun sendEmail(ticket: Ticket, to: String, subject: String, body: String) {
        try {
            val msg = SimpleMailMessage()
            msg.from = emailFrom
            msg.setTo(to)
            msg.subject = subject
            msg.text = body
            mailSender?.send(msg)
            logNotification(ticket, NotificationChannel.EMAIL, to, body, true)
            log.info("Email sent for ticket {} to {}", ticket.id, to)
        } catch (e: Exception) {
            log.error("Email notification failed for ticket {} to {}", ticket.id, to, e)
            logNotification(ticket, NotificationChannel.EMAIL, to, body, false, e.message)
        }
    }

    private fun logNotification(
        ticket: Ticket, channel: NotificationChannel, recipient: String,
        message: String, success: Boolean, error: String? = null
    ) {
        notificationLogRepository.save(
            NotificationLog(
                ticketId = ticket.id ?: "",
                channel = channel,
                recipient = recipient,
                message = message,
                success = success,
                error = error
            )
        )
    }
}
