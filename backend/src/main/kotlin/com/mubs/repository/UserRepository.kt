package com.mubs.repository

import com.mubs.model.User
import com.mubs.model.enums.UserRole
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, String> {
    fun findByUsername(username: String): User?
    fun findByTeam(team: String): List<User>
    fun findByTeamAndRoleAndEnabledTrue(team: String, role: UserRole): List<User>
    fun findByRoleAndEnabledTrue(role: UserRole): List<User>
}
