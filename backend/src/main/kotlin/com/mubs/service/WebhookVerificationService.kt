package com.mubs.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class WebhookVerificationService(
    @Value("\${mubs.hvas.webhook-secret}") private val webhookSecret: String
) {
    fun verifySignature(body: ByteArray, signature: String?): Boolean {
        if (signature.isNullOrBlank()) return false
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(webhookSecret.toByteArray(), "HmacSHA256"))
        val expected = mac.doFinal(body).joinToString("") { "%02x".format(it) }
        return expected == signature
    }
}
