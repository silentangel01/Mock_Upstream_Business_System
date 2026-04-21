package com.mubs.controller

import com.mubs.dto.CreateUserRequest
import com.mubs.dto.ResetPasswordRequest
import com.mubs.dto.UpdateUserRequest
import com.mubs.dto.UserResponse
import com.mubs.model.User
import com.mubs.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
class UserController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @GetMapping
    fun listUsers(): ResponseEntity<List<UserResponse>> {
        val users = userRepository.findAll().map { it.toResponse() }
        return ResponseEntity.ok(users)
    }

    @PostMapping
    fun createUser(@RequestBody req: CreateUserRequest): ResponseEntity<Any> {
        if (userRepository.findByUsername(req.username) != null) {
            return ResponseEntity.badRequest().body(mapOf("message" to "用户名已存在"))
        }
        val user = User(
            username = req.username,
            passwordHash = passwordEncoder.encode(req.password),
            role = req.role,
            team = req.team,
            email = req.email,
            phone = req.phone,
            displayName = req.displayName
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(userRepository.save(user).toResponse())
    }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: String, @RequestBody req: UpdateUserRequest): ResponseEntity<Any> {
        val existing = userRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val updated = existing.copy(
            email = req.email ?: existing.email,
            phone = req.phone ?: existing.phone,
            displayName = req.displayName ?: existing.displayName,
            team = req.team ?: existing.team,
            role = req.role ?: existing.role,
            enabled = req.enabled ?: existing.enabled
        )
        return ResponseEntity.ok(userRepository.save(updated).toResponse())
    }

    @PutMapping("/{id}/password")
    fun resetPassword(@PathVariable id: String, @RequestBody req: ResetPasswordRequest): ResponseEntity<Any> {
        val existing = userRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val updated = existing.copy(passwordHash = passwordEncoder.encode(req.newPassword))
        userRepository.save(updated)
        return ResponseEntity.ok(mapOf("message" to "密码已重置"))
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: String): ResponseEntity<Void> {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build()
        }
        userRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    private fun User.toResponse() = UserResponse(
        id = id!!,
        username = username,
        role = role,
        team = team,
        email = email,
        phone = phone,
        displayName = displayName,
        enabled = enabled
    )
}
