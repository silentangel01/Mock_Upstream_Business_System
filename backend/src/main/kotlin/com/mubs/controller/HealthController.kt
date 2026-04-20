package com.mubs.controller

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HealthController(private val mongoTemplate: MongoTemplate) {

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        val mongoStatus = try {
            mongoTemplate.db.runCommand(org.bson.Document("ping", 1))
            "connected"
        } catch (e: Exception) {
            "disconnected"
        }
        return ResponseEntity.ok(mapOf("status" to "ok", "mongodb" to mongoStatus))
    }
}
