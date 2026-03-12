package com.himetrica.tracker

import android.app.Application
import android.content.Context
import android.util.Log
import com.himetrica.tracker.lifecycle.ActivityTracker
import com.himetrica.tracker.lifecycle.HimetricaLifecycleObserver
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

class Himetrica private constructor(
    private val config: HimetricaConfig,
    context: Context,
) {
    private val appContext: Context = context.applicationContext
    private val storageManager = StorageManager(appContext)
    private val deviceInfo = DeviceInfo(appContext)
    private val userAgent: String =
        "Himetrica-Android/${deviceInfo.appVersion} (${deviceInfo.deviceModel}; Android ${deviceInfo.osVersion})"
    private val networkManager = NetworkManager(config, storageManager, appContext, userAgent)
    private var errorTracking: ErrorTracking? = null

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    // Screen tracking state (guarded by screenLock)
    private val screenLock = Any()
    @Volatile private var currentScreenId: String? = null
    @Volatile private var currentScreenName: String? = null
    @Volatile private var screenStartTime: Long = 0L
    @Volatile private var tapCount: Int = 0

    // Lifecycle
    private var lifecycleObserver: HimetricaLifecycleObserver? = null
    private var activityTracker: ActivityTracker? = null

    // Background tracking (managed by lifecycle observer)
    internal var backgroundAt: Long = 0L

    init {
        setupErrorTracking()
        setupLifecycleObservers()
        log("Initialized with API URL: ${config.apiUrl}")
    }

    // -- Configuration --

    companion object {
        @Volatile
        private var instance: Himetrica? = null

        @JvmStatic
        val shared: Himetrica
            get() = instance ?: throw IllegalStateException(
                "Himetrica.configure() must be called before accessing shared instance"
            )

        @JvmStatic
        fun configure(context: Context, apiKey: String) {
            configure(context, HimetricaConfig(apiKey = apiKey))
        }

        @JvmStatic
        @Synchronized
        fun configure(context: Context, config: HimetricaConfig) {
            if (instance != null) {
                Log.w("Himetrica", "SDK already configured")
                return
            }
            instance = Himetrica(config, context)
        }

        @JvmStatic
        val isInitialized: Boolean get() = instance != null
    }

    // -- Screen Tracking --

    fun trackScreen(name: String, properties: Map<String, Any>? = null) {
        val newScreenId = UUID.randomUUID().toString()

        synchronized(screenLock) {
            // Send duration for previous screen
            sendScreenDurationLocked()

            // Start tracking new screen
            currentScreenId = newScreenId
            currentScreenName = name
            screenStartTime = System.currentTimeMillis()
            tapCount = 0
        }

        val event = ScreenViewEvent(
            visitorId = storageManager.getVisitorId(),
            sessionId = storageManager.getSessionId(config.sessionTimeoutMs),
            pageViewId = newScreenId,
            path = "/${name.lowercase().replace(" ", "-")}",
            title = name,
            referrer = storageManager.getOriginalReferrer(),
            queryString = "",
            screenWidth = deviceInfo.screenWidth,
            screenHeight = deviceInfo.screenHeight,
            platform = "android",
            appVersion = deviceInfo.appVersion,
            osVersion = deviceInfo.osVersion,
            deviceModel = deviceInfo.deviceModel,
            locale = deviceInfo.locale,
        )

        networkManager.sendEvent(endpoint = "/api/track/event", data = event)
        log("Tracked screen: $name")
    }

    fun trackTap() {
        synchronized(screenLock) { tapCount++ }
    }

    // -- Custom Events --

    fun track(name: String, properties: Map<String, Any>? = null) {
        if (!isValidEventName(name)) {
            log("Invalid event name: $name")
            return
        }

        val encodedProperties: Map<String, JsonElement>? = properties?.mapValues { (_, v) ->
            when (v) {
                is String -> JsonPrimitive(v)
                is Number -> JsonPrimitive(v)
                is Boolean -> JsonPrimitive(v)
                else -> JsonPrimitive(v.toString())
            }
        }

        val event = CustomEvent(
            visitorId = storageManager.getVisitorId(),
            sessionId = storageManager.getSessionId(config.sessionTimeoutMs),
            eventName = name,
            properties = encodedProperties,
            path = currentScreenName?.let { "/${it.lowercase().replace(" ", "-")}" } ?: "",
            title = currentScreenName ?: "",
            queryString = "",
            platform = "android",
        )

        networkManager.sendEvent(endpoint = "/api/track/custom-event", data = event)
        log("Tracked event: $name")
    }

    // -- Identification --

    fun identify(userId: String? = null, name: String? = null, email: String? = null, metadata: Map<String, Any>? = null) {
        val encodedMetadata: Map<String, JsonElement>? = metadata?.mapValues { (_, v) ->
            when (v) {
                is String -> JsonPrimitive(v)
                is Number -> JsonPrimitive(v)
                is Boolean -> JsonPrimitive(v)
                else -> JsonPrimitive(v.toString())
            }
        }

        val event = IdentifyEvent(
            visitorId = storageManager.getVisitorId(),
            userId = userId,
            name = name,
            email = email,
            metadata = encodedMetadata,
        )

        val jsonString = json.encodeToString(event)
        networkManager.sendEventWithResponse("/api/track/identify", jsonString) { success, responseBody ->
            if (success && responseBody != null) {
                try {
                    val responseJson = Json.parseToJsonElement(responseBody)
                    val canonicalId = (responseJson as? kotlinx.serialization.json.JsonObject)
                        ?.get("visitorId")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (canonicalId != null && canonicalId != storageManager.getVisitorId()) {
                        storageManager.setVisitorId(canonicalId)
                        log("Visitor ID updated to canonical: $canonicalId")
                    }
                } catch (_: Exception) {
                    // Response parsing is best-effort
                }
            }
        }

        log("Identified user: ${userId ?: name ?: "unknown"}")
    }

    // -- Deep Link Attribution --

    fun setReferrer(url: String) {
        storageManager.setOriginalReferrer(url)
        log("Set referrer: $url")
    }

    // -- Error Tracking --

    fun captureError(throwable: Throwable, context: Map<String, Any>? = null) {
        errorTracking?.captureError(throwable, context, ErrorSeverity.ERROR)
        log("Captured error: ${throwable.message}")
    }

    fun captureMessage(message: String, severity: ErrorSeverity = ErrorSeverity.INFO, context: Map<String, Any>? = null) {
        errorTracking?.captureMessage(message, severity, context)
        log("Captured message: $message")
    }

    // -- Utility --

    val visitorId: String get() = storageManager.getVisitorId()

    fun flush() {
        networkManager.flush()
    }

    fun reset() {
        synchronized(screenLock) { sendScreenDurationLocked() }
        storageManager.reset()
        log("All data reset")
    }

    fun destroy() {
        sendScreenDuration()
        errorTracking?.destroy()
        networkManager.destroy()
        lifecycleObserver?.let {
            androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.removeObserver(it)
        }
        activityTracker?.let {
            (appContext as? Application)?.unregisterActivityLifecycleCallbacks(it)
        }
        instance = null
        log("Destroyed")
    }

    // -- Lifecycle (called by HimetricaLifecycleObserver) --

    internal fun onAppForeground() {
        val awayMs = if (backgroundAt > 0) System.currentTimeMillis() - backgroundAt else 0L
        backgroundAt = 0L

        if (awayMs >= config.sessionTimeoutMs) {
            // Session expired — create new session and re-track current screen
            storageManager.getSessionId(config.sessionTimeoutMs)
            currentScreenName?.let { trackScreen(it) }
        } else if (awayMs > 5 * 60 * 1000L) {
            // Away 5+ min but session still valid — lightweight heartbeat
            storageManager.updateSessionTimestamp()
            sendHeartbeat()
        } else {
            storageManager.updateSessionTimestamp()
        }
        networkManager.flush()
    }

    internal fun onAppBackground() {
        synchronized(screenLock) { sendScreenDurationLocked() }
        backgroundAt = System.currentTimeMillis()
    }

    // -- Private --

    private fun sendHeartbeat() {
        val payload = HeartbeatEvent(
            visitorId = storageManager.getVisitorId(),
            sessionId = storageManager.getSessionId(config.sessionTimeoutMs),
        )
        val jsonString = json.encodeToString(payload)
        networkManager.sendBeacon(
            endpoint = "/api/track/heartbeat?apiKey=${config.apiKey}",
            jsonString = jsonString,
        )
        log("Heartbeat sent")
    }

    internal fun sendScreenDuration() {
        synchronized(screenLock) { sendScreenDurationLocked() }
    }

    /** Must be called while holding [screenLock]. */
    private fun sendScreenDurationLocked() {
        val screenId = currentScreenId ?: return
        val startTime = screenStartTime
        if (startTime == 0L) return

        val durationSec = ((System.currentTimeMillis() - startTime) / 1000).toInt()

        // Only send if duration is reasonable (1 second to 1 hour)
        if (durationSec < 1 || durationSec > 3600) {
            log("Duration out of range: ${durationSec}s")
            currentScreenId = null
            screenStartTime = 0L
            return
        }

        val event = DurationEvent(
            pageViewId = screenId,
            duration = durationSec,
            clickCount = if (tapCount > 0) tapCount else null,
        )

        val jsonString = json.encodeToString(event)
        networkManager.sendBeacon(
            endpoint = "/api/track/beacon?apiKey=${config.apiKey}",
            jsonString = jsonString,
        )

        currentScreenId = null
        screenStartTime = 0L
        log("Sent duration: ${durationSec}s for screen")
    }

    private fun setupErrorTracking() {
        errorTracking = ErrorTracking(
            config = config,
            networkManager = networkManager,
            storageManager = storageManager,
            currentPath = {
                currentScreenName?.let { "/${it.lowercase().replace(" ", "-")}" } ?: ""
            },
            userAgent = userAgent,
        )
    }

    private fun setupLifecycleObservers() {
        // ProcessLifecycleOwner for foreground/background
        lifecycleObserver = HimetricaLifecycleObserver(this)
        try {
            androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver!!)
        } catch (e: Exception) {
            log("Failed to register lifecycle observer: ${e.message}")
        }

        // Activity tracking for auto screen views
        if (config.autoTrackScreenViews) {
            val app = appContext as? Application
            if (app != null) {
                activityTracker = ActivityTracker(this)
                app.registerActivityLifecycleCallbacks(activityTracker)
            }
        }
    }

    private fun isValidEventName(name: String): Boolean {
        if (name.isBlank() || name.length > 255) return false
        return name.matches(Regex("^[a-zA-Z][a-zA-Z0-9_-]*$"))
    }

    private fun log(message: String) {
        if (config.enableLogging) {
            Log.d("Himetrica", message)
        }
    }
}
