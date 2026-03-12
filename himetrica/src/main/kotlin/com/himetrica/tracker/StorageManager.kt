package com.himetrica.tracker

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

internal class StorageManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("himetrica_prefs", Context.MODE_PRIVATE)

    private val queueDir: File =
        File(context.filesDir, "himetrica/queue").also { it.mkdirs() }

    private val json = Json { ignoreUnknownKeys = true }

    // -- Visitor ID (persistent across app launches) --

    fun getVisitorId(): String {
        val existing = prefs.getString(KEY_VISITOR_ID, null)
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_VISITOR_ID, newId).apply()
        return newId
    }

    fun setVisitorId(id: String) {
        prefs.edit().putString(KEY_VISITOR_ID, id).apply()
    }

    // -- Session management --

    fun getSessionId(timeoutMs: Long): String {
        val now = System.currentTimeMillis()
        val lastTimestamp = prefs.getLong(KEY_SESSION_TIMESTAMP, 0L)
        val existingSessionId = prefs.getString(KEY_SESSION_ID, null)

        if (existingSessionId != null && lastTimestamp > 0 && (now - lastTimestamp) < timeoutMs) {
            prefs.edit().putLong(KEY_SESSION_TIMESTAMP, now).apply()
            return existingSessionId
        }

        // Create new session
        val newSessionId = UUID.randomUUID().toString()
        prefs.edit()
            .putString(KEY_SESSION_ID, newSessionId)
            .putLong(KEY_SESSION_TIMESTAMP, now)
            .remove(KEY_ORIGINAL_REFERRER)
            .apply()
        return newSessionId
    }

    fun updateSessionTimestamp() {
        prefs.edit().putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis()).apply()
    }

    // -- Referrer (deep link attribution) --

    fun getOriginalReferrer(): String {
        return prefs.getString(KEY_ORIGINAL_REFERRER, "") ?: ""
    }

    fun setOriginalReferrer(referrer: String) {
        if (prefs.getString(KEY_ORIGINAL_REFERRER, null) == null) {
            prefs.edit().putString(KEY_ORIGINAL_REFERRER, referrer).apply()
        }
    }

    // -- File-based event queue --

    fun enqueueEvent(event: QueuedEvent) {
        try {
            val file = File(queueDir, "${event.id}.json")
            file.writeText(json.encodeToString(event))
        } catch (e: Exception) {
            // Silently fail — analytics are non-critical
        }
    }

    fun dequeueEvents(limit: Int): List<QueuedEvent> {
        val files = queueDir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files
            .sortedBy { it.lastModified() }
            .take(limit)
            .mapNotNull { file ->
                try {
                    json.decodeFromString<QueuedEvent>(file.readText())
                } catch (_: Exception) {
                    file.delete()
                    null
                }
            }
    }

    fun removeEvent(id: String) {
        File(queueDir, "$id.json").delete()
    }

    fun updateEvent(event: QueuedEvent) {
        try {
            val file = File(queueDir, "${event.id}.json")
            file.writeText(json.encodeToString(event))
        } catch (_: Exception) {
            // Silently fail
        }
    }

    fun pruneQueue(maxSize: Int) {
        val files = queueDir.listFiles { f -> f.extension == "json" } ?: return
        if (files.size <= maxSize) return
        files.sortedBy { it.lastModified() }
            .take(files.size - maxSize)
            .forEach { it.delete() }
    }

    fun reset() {
        prefs.edit()
            .remove(KEY_VISITOR_ID)
            .remove(KEY_SESSION_ID)
            .remove(KEY_SESSION_TIMESTAMP)
            .remove(KEY_ORIGINAL_REFERRER)
            .apply()
        queueDir.listFiles()?.forEach { it.delete() }
    }

    companion object {
        private const val KEY_VISITOR_ID = "hm_visitor_id"
        private const val KEY_SESSION_ID = "hm_session_id"
        private const val KEY_SESSION_TIMESTAMP = "hm_session_timestamp"
        private const val KEY_ORIGINAL_REFERRER = "hm_original_referrer"
    }
}
