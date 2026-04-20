package com.mubs.service

import com.mubs.model.NotificationLog
import com.mubs.model.Ticket
import com.mubs.model.enums.NotificationChannel
import com.mubs.repository.NotificationLogRepository
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
    @Value("\${spring.mail.enabled:false}") private val mailEnabled: Boolean,
    @Value("\${mubs.h5.base-url:http://localhost:5173}") private val h5BaseUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun notifyNewTicket(ticket: Ticket) {
        val message = "New ticket [${ticket.id}] — ${ticket.eventType}: ${ticket.description ?: "No description"}"

        // WebSocket broadcast
        sendWebSocket(ticket, message)

        // Email (if enabled)
        if (mailEnabled && ticket.assignedTeam != null) {
            sendEmail(ticket, "New MUBS Ticket Assigned", message)
        }
    }

    fun notifyStatusChange(ticket: Ticket) {
        val message = "Ticket [${ticket.id}] status changed to ${ticket.status}"
        sendWebSocket(ticket, message)
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
            notificationLogRepository.save(
                NotificationLog(
                    ticketId = ticket.id ?: "",
                    channel = NotificationChannel.WEBSOCKET,
                    recipient = "/topic/tickets/all",
                    message = message,
                    success = true
                )
            )
        } catch (e: Exception) {
            log.error("WebSocket notification failed for ticket {}", ticket.id, e)
            notificationLogRepository.save(
                NotificationLog(
                    ticketId = ticket.id ?: "",
                    channel = NotificationChannel.WEBSOCKET,
                    recipient = "/topic/tickets/all",
                    message = message,
                    success = false,
                    error = e.message
                )
            )
        }
    }

    private fun sendEmail(ticket: Ticket, subject: String, body: String) {
        try {
            val msg = SimpleMailMessage()
            msg.setTo("${ticket.assignedTeam}@mubs.local")
            msg.subject = subject
            msg.text = "$body\n\nView ticket: $h5BaseUrl/tickets/${ticket.id}"
            mailSender?.send(msg)
            notificationLogRepository.save(
                NotificationLog(
                    ticketId = ticket.id ?: "",
                    channel = NotificationChannel.EMAIL,
                    recipient = "${ticket.assignedTeam}@mubs.local",
                    message = body,
                    success = true
                )
            )
            log.info("Email sent for ticket {} to {}", ticket.id, ticket.assignedTeam)
        } catch (e: Exception) {
            log.error("Email notification failed for ticket {}", ticket.id, e)
            notificationLogRepository.save(
                NotificationLog(
                    ticketId = ticket.id ?: "",
                    channel = NotificationChannel.EMAIL,
                    recipient = "${ticket.assignedTeam}@mubs.local",
                    message = body,
                    success = false,
                    error = e.message
                )
            )
        }
    }
}
