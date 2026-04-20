package com.mubs.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "dispatch_rules")
data class DispatchRule(
    @Id val id: String? = null,
    val eventType: String,
    val areaCode: String = "*",
    val targetTeam: String,
    val priority: Int = 1,
    val enabled: Boolean = true
)
