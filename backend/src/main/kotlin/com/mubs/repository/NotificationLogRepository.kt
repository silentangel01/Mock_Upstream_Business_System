package com.mubs.repository

import com.mubs.model.NotificationLog
import org.springframework.data.mongodb.repository.MongoRepository

interface NotificationLogRepository : MongoRepository<NotificationLog, String> {
    fun findByTicketId(ticketId: String): List<NotificationLog>
}
