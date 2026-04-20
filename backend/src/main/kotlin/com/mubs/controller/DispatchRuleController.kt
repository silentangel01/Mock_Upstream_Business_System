package com.mubs.controller

import com.mubs.dto.DispatchRuleDto
import com.mubs.model.DispatchRule
import com.mubs.repository.DispatchRuleRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/dispatch-rules")
@PreAuthorize("hasRole('ADMIN')")
class DispatchRuleController(
    private val dispatchRuleRepository: DispatchRuleRepository
) {

    @GetMapping
    fun listRules(): ResponseEntity<List<DispatchRule>> {
        return ResponseEntity.ok(dispatchRuleRepository.findAll())
    }

    @GetMapping("/{id}")
    fun getRule(@PathVariable id: String): ResponseEntity<DispatchRule> {
        return dispatchRuleRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }

    @PostMapping
    fun createRule(@Valid @RequestBody dto: DispatchRuleDto): ResponseEntity<DispatchRule> {
        val rule = DispatchRule(
            eventType = dto.eventType,
            areaCode = dto.areaCode,
            targetTeam = dto.targetTeam,
            priority = dto.priority,
            enabled = dto.enabled
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(dispatchRuleRepository.save(rule))
    }

    @PutMapping("/{id}")
    fun updateRule(
        @PathVariable id: String,
        @Valid @RequestBody dto: DispatchRuleDto
    ): ResponseEntity<DispatchRule> {
        if (!dispatchRuleRepository.existsById(id)) {
            return ResponseEntity.notFound().build()
        }
        val rule = DispatchRule(
            id = id,
            eventType = dto.eventType,
            areaCode = dto.areaCode,
            targetTeam = dto.targetTeam,
            priority = dto.priority,
            enabled = dto.enabled
        )
        return ResponseEntity.ok(dispatchRuleRepository.save(rule))
    }

    @DeleteMapping("/{id}")
    fun deleteRule(@PathVariable id: String): ResponseEntity<Void> {
        if (!dispatchRuleRepository.existsById(id)) {
            return ResponseEntity.notFound().build()
        }
        dispatchRuleRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
