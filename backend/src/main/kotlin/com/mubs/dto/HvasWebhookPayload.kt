package com.mubs.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class HvasWebhookPayload(
    @JsonProperty("event_id") val eventId: String,
    @JsonProperty("event_type") val eventType: String,
    @JsonProperty("camera_id") val cameraId: String,
    val timestamp: Double,
    @JsonProperty("created_at") val createdAt: String,
    val confidence: Double,
    @JsonProperty("image_url") val imageUrl: String? = null,
    val description: String? = null,
    @JsonProperty("object_count") val objectCount: Int? = null,
    @JsonProperty("lat_lng") val latLng: String? = null,
    val location: String? = null,
    @JsonProperty("area_code") val areaCode: String? = null,
    val group: String? = null
)
