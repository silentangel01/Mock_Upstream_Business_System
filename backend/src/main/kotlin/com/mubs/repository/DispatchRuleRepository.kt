package com.mubs.repository

import com.mubs.model.DispatchRule
import org.springframework.data.mongodb.repository.MongoRepository

interface DispatchRuleRepository : MongoRepository<DispatchRule, String> {
    fun findByEventTypeAndEnabledTrueOrderByPriorityDesc(eventType: String): List<DispatchRule>
    fun findByEnabledTrue(): List<DispatchRule>
}
