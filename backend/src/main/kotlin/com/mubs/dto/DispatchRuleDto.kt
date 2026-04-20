package com.mubs.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class DispatchRuleDto(
    @field:NotBlank val eventType: String,
    val areaCode: String = "*",
    @field:NotBlank val targetTeam: String,
    @field:Min(1) val priority: Int = 1,
    val enabled: Boolean = true
)
