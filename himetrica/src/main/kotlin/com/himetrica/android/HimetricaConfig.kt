package com.himetrica.android

/**
 * Configuration for the Himetrica SDK.
 *
 * Use the [Builder] for Java interop, or the constructor directly from Kotlin.
 */
data class HimetricaConfig(
    /** The API key for your Himetrica project. */
    val apiKey: String,
    /** The base URL for the Himetrica API. */
    val apiUrl: String = "https://app.himetrica.com",
    /** Session timeout in milliseconds (default 30 minutes). */
    val sessionTimeoutMs: Long = 30 * 60 * 1000L,
    /** Automatically track Activity screen views. */
    val autoTrackScreenViews: Boolean = true,
    /** Enable debug logging. */
    val enableLogging: Boolean = false,
    /** Maximum number of events to queue when offline. */
    val maxQueueSize: Int = 1000,
    /** Interval for flushing the event queue in milliseconds (default 30s). */
    val flushIntervalMs: Long = 30_000L,
    /** Capture uncaught exceptions. */
    val captureUncaughtExceptions: Boolean = true,
    /** Maximum errors per rate limit window. */
    val errorRateLimit: Int = 10,
    /** Rate limit window in milliseconds (default 60s). */
    val errorRateLimitWindowMs: Long = 60_000L,
) {
    init {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        require(apiUrl.isNotBlank()) { "apiUrl must not be blank" }
        require(sessionTimeoutMs > 0) { "sessionTimeoutMs must be positive" }
        require(maxQueueSize > 0) { "maxQueueSize must be positive" }
        require(flushIntervalMs > 0) { "flushIntervalMs must be positive" }
    }

    class Builder(private val apiKey: String) {
        private var apiUrl: String = "https://app.himetrica.com"
        private var sessionTimeoutMs: Long = 30 * 60 * 1000L
        private var autoTrackScreenViews: Boolean = true
        private var enableLogging: Boolean = false
        private var maxQueueSize: Int = 1000
        private var flushIntervalMs: Long = 30_000L
        private var captureUncaughtExceptions: Boolean = true
        private var errorRateLimit: Int = 10
        private var errorRateLimitWindowMs: Long = 60_000L

        fun apiUrl(url: String) = apply { apiUrl = url }
        fun sessionTimeoutMs(ms: Long) = apply { sessionTimeoutMs = ms }
        fun autoTrackScreenViews(enabled: Boolean) = apply { autoTrackScreenViews = enabled }
        fun enableLogging(enabled: Boolean) = apply { enableLogging = enabled }
        fun maxQueueSize(size: Int) = apply { maxQueueSize = size }
        fun flushIntervalMs(ms: Long) = apply { flushIntervalMs = ms }
        fun captureUncaughtExceptions(enabled: Boolean) = apply { captureUncaughtExceptions = enabled }
        fun errorRateLimit(limit: Int) = apply { errorRateLimit = limit }
        fun errorRateLimitWindowMs(ms: Long) = apply { errorRateLimitWindowMs = ms }

        fun build() = HimetricaConfig(
            apiKey = apiKey,
            apiUrl = apiUrl,
            sessionTimeoutMs = sessionTimeoutMs,
            autoTrackScreenViews = autoTrackScreenViews,
            enableLogging = enableLogging,
            maxQueueSize = maxQueueSize,
            flushIntervalMs = flushIntervalMs,
            captureUncaughtExceptions = captureUncaughtExceptions,
            errorRateLimit = errorRateLimit,
            errorRateLimitWindowMs = errorRateLimitWindowMs,
        )
    }
}
