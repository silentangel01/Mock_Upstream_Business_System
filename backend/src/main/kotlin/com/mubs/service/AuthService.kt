package com.mubs.service

import com.mubs.dto.LoginRequest
import com.mubs.dto.LoginResponse
import com.mubs.repository.UserRepository
import com.mubs.security.JwtTokenProvider
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) {
    fun login(request: LoginRequest): LoginResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )
        val user = userRepository.findByUsername(request.username)
            ?: throw IllegalStateException("User not found after authentication")
        val token = jwtTokenProvider.generateToken(user.username, user.role.name)
        return LoginResponse(
            token = token,
            username = user.username,
            role = user.role.name
        )
    }
}
