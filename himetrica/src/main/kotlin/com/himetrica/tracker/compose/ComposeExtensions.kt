package com.himetrica.tracker.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.himetrica.tracker.Himetrica

/**
 * Tracks a screen view when this composable enters composition.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     TrackScreen("HomeScreen")
 *     // ... your content
 * }
 * ```
 */
@Composable
fun TrackScreen(name: String, properties: Map<String, Any>? = null) {
    LaunchedEffect(name) {
        if (Himetrica.isInitialized) {
            Himetrica.shared.trackScreen(name, properties)
        }
    }
}

/**
 * Composable that tracks a screen view with automatic duration on dispose.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     TrackScreenWithDuration("HomeScreen")
 *     // ... your content
 * }
 * ```
 */
@Composable
fun TrackScreenWithDuration(name: String, properties: Map<String, Any>? = null) {
    DisposableEffect(name) {
        if (Himetrica.isInitialized) {
            Himetrica.shared.trackScreen(name, properties)
        }
        onDispose {
            if (Himetrica.isInitialized) {
                Himetrica.shared.sendScreenDuration()
            }
        }
    }
}
