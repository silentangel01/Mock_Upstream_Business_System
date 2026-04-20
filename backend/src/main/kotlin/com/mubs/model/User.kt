package com.mubs.model

import com.mubs.model.enums.UserRole
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "users")
data class User(
    @Id val id: String? = null,
    @Indexed(unique = true) val username: String,
    val passwordHash: String,
    val role: UserRole,
    val team: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val enabled: Boolean = true
)
