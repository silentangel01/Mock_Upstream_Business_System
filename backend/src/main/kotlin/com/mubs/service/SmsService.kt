package com.mubs.service

import com.aliyun.auth.credentials.Credential
import com.aliyun.auth.credentials.provider.StaticCredentialProvider
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest
import com.fasterxml.jackson.databind.ObjectMapper
import darabonba.core.client.ClientOverrideConfiguration
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SmsService(
    private val objectMapper: ObjectMapper,
    @Value("\${mubs.notification.sms.enabled:false}") private val smsEnabled: Boolean,
    @Value("\${mubs.notification.sms.access-key-id:}") private val accessKeyId: String,
    @Value("\${mubs.notification.sms.access-key-secret:}") private val accessKeySecret: String,
    @Value("\${mubs.notification.sms.sign-name:MUBS城管}") private val signName: String,
    @Value("\${mubs.notification.sms.template-code.dispatch:SMS_000001}") private val dispatchTemplate: String,
    @Value("\${mubs.notification.sms.template-code.timeout:SMS_000002}") private val timeoutTemplate: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private var client: AsyncClient? = null

    @PostConstruct
    fun init() {
        if (!smsEnabled) {
            log.info("SMS notifications disabled")
            return
        }
        if (accessKeyId.isBlank() || accessKeySecret.isBlank()) {
            log.warn("SMS enabled but credentials not configured, SMS will not be sent")
            return
        }
        val credentialProvider = StaticCredentialProvider.create(
            Credential.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .build()
        )
        client = AsyncClient.builder()
            .credentialsProvider(credentialProvider)
            .overrideConfiguration(
                ClientOverrideConfiguration.create()
                    .setEndpointOverride("dysmsapi.aliyuncs.com")
            )
            .build()
        log.info("Aliyun SMS client initialized")
    }

    fun isEnabled(): Boolean = smsEnabled && client != null

    fun getDispatchTemplate(): String = dispatchTemplate
    fun getTimeoutTemplate(): String = timeoutTemplate

    fun sendSms(phone: String, templateCode: String, templateParams: Map<String, String>): Boolean {
        if (!isEnabled()) {
            log.debug("SMS disabled, skipping send to {}", phone)
            return false
        }
        return try {
            val request = SendSmsRequest.builder()
                .phoneNumbers(phone)
                .signName(signName)
                .templateCode(templateCode)
                .templateParam(objectMapper.writeValueAsString(templateParams))
                .build()
            val future = client!!.sendSms(request)
            val response = future.get()
            val code = response.body?.code
            if (code == "OK") {
                log.info("SMS sent to {} with template ", phone, templateCode)
                true
            } else {
                log.warn("SMS send failed for {}: {} - {}", phone, code, response.body?.message)
                false
            }
        } catch (e: Exception) {
            log.error("SMS send error for {}", phone, e)
            false
        }
    }
}
