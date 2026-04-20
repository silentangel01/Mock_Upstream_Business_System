package com.mubs.model

import com.mubs.model.enums.NotificationChannel
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "notification_logs")
data class NotificationLog(
    @Id val id: String? = null,
    val ticketId: String,
    val channel: NotificationChannel,
    val recipient: String,
    val message: String,
    val success: Boolean,
    val error: String? = null,
    val sentAt: Instant = Instant.now()
)
