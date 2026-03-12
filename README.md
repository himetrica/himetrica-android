# Himetrica Android SDK

Native Android analytics SDK for [Himetrica](https://app.himetrica.com). Mirrors the iOS (Swift) SDK's architecture adapted to Kotlin/Android idioms.

## Installation

Add the library module to your project. (Maven Central publishing coming soon.)

```groovy
// settings.gradle.kts
include(":himetrica")
project(":himetrica").projectDir = file("path/to/himetrica")

// app/build.gradle.kts
dependencies {
    implementation(project(":himetrica"))
}
```

## Quick Start

### Initialize (Application.onCreate)

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Himetrica.configure(this, "hm_pk_your_api_key")
    }
}
```

Or with full configuration:

```kotlin
Himetrica.configure(this, HimetricaConfig(
    apiKey = "hm_pk_your_api_key",
    apiUrl = "https://app.himetrica.com",
    sessionTimeoutMs = 30 * 60 * 1000L,
    autoTrackScreenViews = true,
    enableLogging = BuildConfig.DEBUG,
))
```

### Track Screens

Screens are tracked automatically via `ActivityLifecycleCallbacks` when `autoTrackScreenViews = true`. For manual tracking:

```kotlin
Himetrica.shared.trackScreen("HomeScreen", mapOf("section" to "featured"))
```

### Track Custom Events

```kotlin
Himetrica.shared.track("purchase", mapOf("amount" to 99.99, "currency" to "USD"))
```

### Identify Users

```kotlin
Himetrica.shared.identify(
    userId = "user_123",
    email = "user@example.com",
    metadata = mapOf("plan" to "pro")
)
```

### Error Tracking

Capture errors manually in your catch blocks. Errors are rate-limited (max 10/minute) and deduplicated (5-minute window).

```kotlin
try {
    // risky operation
} catch (e: Exception) {
    Himetrica.shared.captureError(e, mapOf("screen" to "checkout"))
}

Himetrica.shared.captureMessage("User exceeded rate limit", ErrorSeverity.WARNING)
```

### Deep Link Attribution

```kotlin
Himetrica.shared.setReferrer("https://campaign.example.com/spring-sale")
```

### Jetpack Compose

```kotlin
@Composable
fun HomeScreen() {
    TrackScreen("HomeScreen")
    // ... your content
}

// Or as a modifier
Box(modifier = Modifier.trackScreen("HomeScreen")) {
    // ... your content
}
```

## Offline Support

Events are queued to a file-based queue when offline and flushed automatically when connectivity is restored. The queue persists across process restarts.

## Architecture

| Class | Responsibility |
|---|---|
| `Himetrica` | Singleton facade, session management, screen duration |
| `HimetricaConfig` | Configuration data class with Builder for Java |
| `StorageManager` | SharedPreferences + file-based event queue |
| `NetworkManager` | OkHttp transport, queue flush, connectivity monitoring |
| `ErrorTracking` | Manual error capture, rate limiting, deduplication |
| `HimetricaLifecycleObserver` | ProcessLifecycleOwner for foreground/background |
| `ActivityTracker` | Auto screen tracking via ActivityLifecycleCallbacks |
| `ComposeExtensions` | `TrackScreen` composable + `Modifier.trackScreen` |

## Requirements

- **minSdk**: 21 (Android 5.0)
- **compileSdk**: 34
- Compose extensions require Jetpack Compose (optional)
