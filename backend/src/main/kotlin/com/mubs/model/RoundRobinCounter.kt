package com.mubs.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "round_robin_counters")
data class RoundRobinCounter(
    @Id val team: String,
    var lastIndex: Int = 0
)
