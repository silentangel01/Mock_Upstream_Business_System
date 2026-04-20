package com.mubs.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WebhookVerificationServiceTest {

    private val secret = "test-webhook-secret"
    private val service = WebhookVerificationService(secret)

    @Test
    fun `should verify valid signature`() {
        val body = """{"event_id":"123","event_type":"smoke_flame"}""".toByteArray()
        val signature = computeHmac(body, secret)
        assertTrue(service.verifySignature(body, signature))
    }

    @Test
    fun `should reject invalid signature`() {
        val body = """{"event_id":"123"}""".toByteArray()
        assertFalse(service.verifySignature(body, "invalid_signature"))
    }

    @Test
    fun `should reject null signature`() {
        val body = """{"event_id":"123"}""".toByteArray()
        assertFalse(service.verifySignature(body, null))
    }

    @Test
    fun `should reject empty signature`() {
        val body = """{"event_id":"123"}""".toByteArray()
        assertFalse(service.verifySignature(body, ""))
    }

    private fun computeHmac(body: ByteArray, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(body).joinToString("") { "%02x".format(it) }
    }
}
