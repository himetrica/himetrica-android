# Himetrica Android SDK

Native Android analytics SDK for [Himetrica](https://app.himetrica.com).

## Installation

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.himetrica.tracker:himetrica-android:0.1.0")
}
```

Make sure Maven Central is in your repositories (`mavenCentral()` in `settings.gradle.kts`).

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
    captureUncaughtExceptions = true,
))
```

Java:

```java
HimetricaConfig config = new HimetricaConfig.Builder("hm_pk_your_api_key")
    .enableLogging(BuildConfig.DEBUG)
    .build();
Himetrica.configure(this, config);
```

### Track Screens

Screens are tracked automatically via `ActivityLifecycleCallbacks` when `autoTrackScreenViews = true`. For manual tracking:

```kotlin
Himetrica.shared.trackScreen("HomeScreen")
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

Uncaught exceptions are captured automatically. For manual capture:

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

// Or with automatic duration tracking on dispose
@Composable
fun HomeScreen() {
    TrackScreenWithDuration("HomeScreen")
    // ... your content
}
```

## Offline Support

Events are queued to a file-based queue when offline and flushed automatically when connectivity is restored. The queue persists across process restarts.

## Requirements

- **minSdk**: 21 (Android 5.0)
- **compileSdk**: 35
- Compose extensions require Jetpack Compose (optional — the SDK works without it)
