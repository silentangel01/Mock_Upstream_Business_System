package com.mubs.repository

import com.mubs.model.User
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, String> {
    fun findByUsername(username: String): User?
    fun findByTeam(team: String): List<User>
}
