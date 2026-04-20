package com.mubs.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JwtTokenProviderTest {

    private val secret = "mubs-test-jwt-secret-key-minimum-32-characters!!"
    private val provider = JwtTokenProvider(secret, 86400000L)

    @Test
    fun `should generate and validate token`() {
        val token = provider.generateToken("admin", "ADMIN")
        assertTrue(provider.validateToken(token))
        assertEquals("admin", provider.getUsernameFromToken(token))
    }

    @Test
    fun `should reject invalid token`() {
        assertFalse(provider.validateToken("invalid.token.here"))
    }

    @Test
    fun `should reject expired token`() {
        val shortLivedProvider = JwtTokenProvider(secret, -1000L)
        val token = shortLivedProvider.generateToken("admin", "ADMIN")
        assertFalse(provider.validateToken(token))
    }
}
