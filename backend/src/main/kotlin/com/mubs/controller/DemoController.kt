package com.mubs.controller

import com.mubs.dto.DemoEventRequest
import com.mubs.model.Ticket
import com.mubs.service.DemoService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/demo")
@ConditionalOnProperty(name = ["mubs.demo.enabled"], havingValue = "true", matchIfMissing = true)
class DemoController(
    private val demoService: DemoService
) {

    @PostMapping("/simulate-event")
    @PreAuthorize("hasRole('ADMIN')")
    fun simulateEvent(@RequestBody(required = false) request: DemoEventRequest?): ResponseEntity<Ticket> {
        val ticket = demoService.simulateEvent(request)
        return ResponseEntity.status(201).body(ticket)
    }
}
