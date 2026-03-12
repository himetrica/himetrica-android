package com.himetrica.tracker

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal class ErrorTracking(
    private val config: HimetricaConfig,
    private val networkManager: NetworkManager,
    private val storageManager: StorageManager,
    private val currentPath: () -> String,
    private val userAgent: String,
) {
    // Rate limiting
    private val errorTimestamps = mutableListOf<Long>()
    private val rateLimitLock = Any()

    // Deduplication
    private val sentErrorHashes = mutableSetOf<String>()
    private val dedupLock = Any()
    private val dedupExpiryMs = 5 * 60 * 1000L
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "himetrica-dedup").apply { isDaemon = true }
    }

    fun destroy() {
        scheduler.shutdown()
    }

    fun captureError(throwable: Throwable, context: Map<String, Any>?, severity: ErrorSeverity) {
        val stack = throwable.stackTrace
            .take(20)
            .joinToString("\n") { "    at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }

        sendErrorEvent(
            type = "error",
            message = "${throwable.javaClass.simpleName}: ${throwable.message ?: "Unknown error"}",
            stack = stack,
            source = throwable.javaClass.name,
            severity = severity,
            context = context,
        )
    }

    fun captureMessage(message: String, severity: ErrorSeverity, context: Map<String, Any>?) {
        sendErrorEvent(
            type = "console",
            message = message,
            stack = null,
            source = null,
            severity = severity,
            context = context,
        )
    }

    // -- Rate Limiting --

    private fun isRateLimited(): Boolean {
        synchronized(rateLimitLock) {
            val now = System.currentTimeMillis()
            errorTimestamps.removeAll { it < now - config.errorRateLimitWindowMs }
            if (errorTimestamps.size >= config.errorRateLimit) return true
            errorTimestamps.add(now)
            return false
        }
    }

    // -- Deduplication --

    private fun isDuplicate(hash: String): Boolean {
        synchronized(dedupLock) {
            if (sentErrorHashes.contains(hash)) return true
            sentErrorHashes.add(hash)

            scheduler.schedule({
                synchronized(dedupLock) {
                    sentErrorHashes.remove(hash)
                }
            }, dedupExpiryMs, TimeUnit.MILLISECONDS)

            return false
        }
    }

    private fun hashError(message: String, stack: String?, source: String?): String {
        val str = "$message|${stack ?: ""}|${source ?: ""}"
        var hash = 0
        for (b in str.toByteArray(Charsets.UTF_8)) {
            hash = ((hash shl 5) - hash) + b.toInt()
        }
        return hash.toString(16)
    }

    // -- Send --

    private fun sendErrorEvent(
        type: String,
        message: String,
        stack: String?,
        source: String?,
        severity: ErrorSeverity,
        context: Map<String, Any>?,
    ) {
        val hash = hashError(message, stack, source)
        if (isRateLimited()) return
        if (isDuplicate(hash)) return

        val encodedContext: Map<String, JsonElement>? = context?.mapValues { (_, v) ->
            when (v) {
                is String -> JsonPrimitive(v)
                is Number -> JsonPrimitive(v)
                is Boolean -> JsonPrimitive(v)
                else -> JsonPrimitive(v.toString())
            }
        }

        val event = ErrorEvent(
            visitorId = storageManager.getVisitorId(),
            sessionId = storageManager.getSessionId(config.sessionTimeoutMs),
            type = type,
            message = message,
            stack = stack,
            source = source,
            severity = severity.value,
            path = currentPath(),
            userAgent = userAgent,
            timestamp = System.currentTimeMillis(),
            context = encodedContext,
        )

        networkManager.sendEvent<ErrorEvent>(
            endpoint = "/api/track/errors?apiKey=${config.apiKey}",
            data = event,
        )
    }
}
