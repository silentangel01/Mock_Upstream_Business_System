package com.mubs.service

import com.mubs.model.NotificationLog
import com.mubs.model.Ticket
import com.mubs.model.enums.NotificationChannel
import com.mubs.model.enums.UserRole
import com.mubs.repository.NotificationLogRepository
import com.mubs.repository.UserRepository
import jakarta.mail.internet.InternetAddress
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Service
class NotificationService(
    private val notificationLogRepository: NotificationLogRepository,
    private val messagingTemplate: SimpMessagingTemplate,
    private val mailSender: JavaMailSender?,
    private val smsService: SmsService,
    private val userRepository: UserRepository,
    private val templateEngine: TemplateEngine,
    @Value("\${mubs.notification.email.enabled:false}") private val emailEnabled: Boolean,
    @Value("\${mubs.notification.email.from:noreply@yourdomain.com}") private val emailFrom: String,
    @Value("\${mubs.notification.email.from-name:MUBS城管系统}") private val emailFromName: String,
    @Value("\${mubs.h5.base-url:http://localhost:5173}") private val h5BaseUrl: String,
    @Value("\${mubs.dispatch.timeout-minutes:30}") private val timeoutMinutes: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun notifyNewTicket(ticket: Ticket) {
        val message = "New ticket [${ticket.id}] — ${ticket.eventType}: ${ticket.description ?: "No description"}"

        // 1. WebSocket broadcast
        sendWebSocket(ticket, message)

        // If assigned to a specific user, notify only that user; otherwise fall back to team
        val targetUser = ticket.assignedUser?.let { userRepository.findByUsername(it) }

        if (targetUser != null) {
            // 2. Email to assigned user
            if (emailEnabled && !targetUser.email.isNullOrBlank()) {
                val subject = "新工单通知：${ticket.eventType}"
                val ctx = Context().apply {
                    setVariable("ticketId", ticket.id ?: "")
                    setVariable("eventType", ticket.eventType)
                    setVariable("description", ticket.description ?: "—")
                    setVariable("team", ticket.assignedTeam ?: "")
                    setVariable("actionUrl", buildTicketUrl(ticket, targetUser.role))
                }
                val html = templateEngine.process("email/new-ticket", ctx)
                sendHtmlEmail(ticket, targetUser.email!!, subject, html)
            }

            // 3. SMS to assigned user
            if (smsService.isEnabled() && !targetUser.phone.isNullOrBlank()) {
                val params = mapOf(
                    "ticketId" to (ticket.id ?: ""),
                    "eventType" to ticket.eventType,
                    "description" to (ticket.description?.take(20) ?: "")
                )
                val success = smsService.sendSms(
                    targetUser.phone!!, smsService.getDispatchTemplate(), params
                )
                logNotification(ticket, NotificationChannel.SMS, targetUser.phone!!, message, success)
            }
        } else {
            // Fallback: notify entire team
            val team = ticket.assignedTeam ?: return
            val teamUsers = userRepository.findByTeam(team)

            if (emailEnabled) {
                val subject = "新工单通知：${ticket.eventType}"
                teamUsers.filter { !it.email.isNullOrBlank() }.forEach { user ->
                    val ctx = Context().apply {
                        setVariable("ticketId", ticket.id ?: "")
                        setVariable("eventType", ticket.eventType)
                        setVariable("description", ticket.description ?: "—")
                        setVariable("team", team)
                        setVariable("actionUrl", buildTicketUrl(ticket, user.role))
                    }
                    val html = templateEngine.process("email/new-ticket", ctx)
                    sendHtmlEmail(ticket, user.email!!, subject, html)
                }
            }

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
            val subject = "工单超时提醒：${ticket.eventType}"
            val ctx = Context().apply {
                setVariable("ticketId", ticket.id ?: "")
                setVariable("eventType", ticket.eventType)
                setVariable("description", ticket.description ?: "—")
                setVariable("timeoutMinutes", timeoutMinutes)
                setVariable("actionUrl", buildTicketUrl(ticket, UserRole.ADMIN))
            }
            val html = templateEngine.process("email/dispatch-timeout", ctx)
            admins.forEach { admin ->
                sendHtmlEmail(ticket, admin.email!!, subject, html)
            }
        }

        // 3. SMS to assigned user (or fall back to first fieldworker in team)
        if (smsService.isEnabled()) {
            val targetUser = ticket.assignedUser?.let { userRepository.findByUsername(it) }
            val smsTarget = targetUser
                ?: ticket.assignedTeam?.let { team ->
                    userRepository.findByTeam(team)
                        .firstOrNull { it.role == UserRole.FIELDWORKER && !it.phone.isNullOrBlank() }
                }

            if (smsTarget != null && !smsTarget.phone.isNullOrBlank()) {
                val params = mapOf(
                    "ticketId" to (ticket.id ?: ""),
                    "eventType" to ticket.eventType
                )
                val success = smsService.sendSms(
                    smsTarget.phone!!, smsService.getTimeoutTemplate(), params
                )
                logNotification(ticket, NotificationChannel.SMS, smsTarget.phone!!, message, success)
            }
        }
    }

    private fun buildTicketUrl(ticket: Ticket, role: UserRole?): String {
        val ticketId = ticket.id ?: ""
        return when (role) {
            UserRole.FIELDWORKER -> "mubs://tickets/$ticketId"
            else -> "$h5BaseUrl/tickets/$ticketId"
        }
    }

    private fun sendWebSocket(ticket: Ticket, message: String) {
        try {
            val payload = mapOf(
                "ticket" to ticket,
                "message" to message,
                "actionUrl" to buildTicketUrl(ticket, null)
            )
            messagingTemplate.convertAndSend("/topic/tickets/all", payload)
            if (ticket.assignedTeam != null) {
                messagingTemplate.convertAndSend("/topic/tickets/${ticket.assignedTeam}", payload)
            }
            if (ticket.assignedUser != null) {
                messagingTemplate.convertAndSend("/topic/tickets/user/${ticket.assignedUser}", payload)
            }
            logNotification(ticket, NotificationChannel.WEBSOCKET, "/topic/tickets/all", message, true)
        } catch (e: Exception) {
            log.error("WebSocket notification failed for ticket {}", ticket.id, e)
            logNotification(ticket, NotificationChannel.WEBSOCKET, "/topic/tickets/all", message, false, e.message)
        }
    }

    private fun sendHtmlEmail(ticket: Ticket, to: String, subject: String, html: String) {
        try {
            val mimeMessage = mailSender!!.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")
            helper.setFrom(InternetAddress(emailFrom, emailFromName, "UTF-8"))
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(html, true)
            mailSender.send(mimeMessage)
            logNotification(ticket, NotificationChannel.EMAIL, to, subject, true)
            log.info("Email sent for ticket {} to {}", ticket.id, to)
        } catch (e: Exception) {
            log.error("Email notification failed for ticket {} to {}", ticket.id, to, e)
            logNotification(ticket, NotificationChannel.EMAIL, to, subject, false, e.message)
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
