package com.mubs.dto

import jakarta.validation.constraints.NotBlank

data class ReassignRequest(
    @field:NotBlank val targetUser: String,
    val note: String? = null
)
