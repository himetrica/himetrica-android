package com.himetrica.android

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ScreenViewEvent(
    val visitorId: String,
    val sessionId: String,
    val pageViewId: String,
    val path: String,
    val title: String,
    val referrer: String,
    val queryString: String,
    val screenWidth: Int,
    val screenHeight: Int,
    val platform: String,
    val appVersion: String,
    val osVersion: String,
    val deviceModel: String,
    val locale: String,
)

@Serializable
internal data class DurationEvent(
    val pageViewId: String,
    val duration: Int,
    val clickCount: Int? = null,
)

@Serializable
internal data class CustomEvent(
    val visitorId: String,
    val sessionId: String,
    val eventName: String,
    val properties: Map<String, JsonElement>? = null,
    val path: String,
    val title: String,
    val queryString: String,
    val platform: String,
)

@Serializable
internal data class HeartbeatEvent(
    val visitorId: String,
    val sessionId: String,
)

@Serializable
internal data class IdentifyEvent(
    val visitorId: String,
    val userId: String? = null,
    val name: String? = null,
    val email: String? = null,
    val metadata: Map<String, JsonElement>? = null,
)

@Serializable
internal data class ErrorEvent(
    val visitorId: String,
    val sessionId: String,
    val type: String,
    val message: String,
    val stack: String? = null,
    val source: String? = null,
    val lineno: Int? = null,
    val colno: Int? = null,
    val severity: String,
    val path: String,
    val userAgent: String,
    val timestamp: Long,
    val context: Map<String, JsonElement>? = null,
)

@Serializable
internal data class QueuedEvent(
    val id: String,
    val endpoint: String,
    val data: String, // JSON string of the event payload
    val timestamp: Long,
    val retryCount: Int = 0,
) {
    fun incrementingRetry(): QueuedEvent = copy(retryCount = retryCount + 1)
}

enum class ErrorSeverity(val value: String) {
    ERROR("error"),
    WARNING("warning"),
    INFO("info"),
}
