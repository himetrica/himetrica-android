package com.himetrica.tracker

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class NetworkManager(
    private val config: HimetricaConfig,
    private val storageManager: StorageManager,
    context: Context,
    private val userAgent: String = "Himetrica-Android",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val handler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "himetrica-network").apply { isDaemon = true }
    }
    @Volatile
    private var isOnline = true
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val flushRunnable = object : Runnable {
        override fun run() {
            flush()
            handler.postDelayed(this, config.flushIntervalMs)
        }
    }

    init {
        setupConnectivityMonitoring(context)
        handler.postDelayed(flushRunnable, config.flushIntervalMs)
    }

    // -- Public --

    inline fun <reified T> sendEvent(endpoint: String, data: T, crossinline onResult: (Boolean) -> Unit = {}) {
        val jsonString: String
        try {
            jsonString = json.encodeToString(data)
        } catch (e: Exception) {
            log("Failed to encode event: ${e.message}")
            onResult(false)
            return
        }

        if (isOnline) {
            performRequest(endpoint, jsonString) { success ->
                if (!success) {
                    queueEvent(endpoint, jsonString)
                }
                onResult(success)
            }
        } else {
            queueEvent(endpoint, jsonString)
            onResult(false)
        }
    }

    fun sendBeacon(endpoint: String, jsonString: String) {
        if (isOnline) {
            performRequest(endpoint, jsonString) { success ->
                if (!success) {
                    queueEvent(endpoint, jsonString)
                }
            }
        } else {
            queueEvent(endpoint, jsonString)
        }
    }

    fun flush() {
        executor.execute { processQueue() }
    }

    fun destroy() {
        handler.removeCallbacks(flushRunnable)
        executor.shutdown()
    }

    // -- Private --

    private fun performRequest(endpoint: String, jsonBody: String, callback: (Boolean) -> Unit) {
        val url = "${config.apiUrl}$endpoint"

        val body = jsonBody.toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/json")
            .header("X-API-Key", config.apiKey)
            .header("User-Agent", userAgent)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                log("Request failed: ${e.message}")
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val success = response.isSuccessful
                    if (!success) {
                        log("Request failed with status: ${response.code}")
                    }
                    callback(success)
                }
            }
        })
    }

    /** Perform a request that returns a response body string (used for identify). */
    fun sendEventWithResponse(endpoint: String, jsonBody: String, callback: (Boolean, String?) -> Unit) {
        val url = "${config.apiUrl}$endpoint"

        val body = jsonBody.toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/json")
            .header("X-API-Key", config.apiKey)
            .header("User-Agent", userAgent)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                log("Request failed: ${e.message}")
                callback(false, null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = response.body?.string()
                    callback(response.isSuccessful, responseBody)
                }
            }
        })
    }

    private fun queueEvent(endpoint: String, jsonBody: String) {
        val event = QueuedEvent(
            id = UUID.randomUUID().toString(),
            endpoint = endpoint,
            data = jsonBody,
            timestamp = System.currentTimeMillis(),
        )
        storageManager.enqueueEvent(event)
        storageManager.pruneQueue(config.maxQueueSize)
        log("Event queued for later delivery")
    }

    private fun processQueue() {
        if (!isOnline) return

        val events = storageManager.dequeueEvents(50)
        if (events.isEmpty()) return

        log("Processing ${events.size} queued events")

        for (event in events) {
            val latch = java.util.concurrent.CountDownLatch(1)
            performRequest(event.endpoint, event.data) { success ->
                if (success) {
                    storageManager.removeEvent(event.id)
                } else if (event.retryCount < 3) {
                    storageManager.updateEvent(event.incrementingRetry())
                } else {
                    storageManager.removeEvent(event.id)
                    log("Event discarded after max retries")
                }
                latch.countDown()
            }
            latch.await(10, TimeUnit.SECONDS)
        }
    }

    private fun setupConnectivityMonitoring(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
                flush()
            }

            override fun onLost(network: Network) {
                isOnline = false
            }
        })
    }

    private fun log(message: String) {
        if (config.enableLogging) {
            android.util.Log.d("Himetrica", message)
        }
    }
}
