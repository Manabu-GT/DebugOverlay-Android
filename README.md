DebugOverlay-Android
====================
[![Maven Central (Snapshots)](https://img.shields.io/maven-metadata/v.svg?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fcom%2Fms-square%2Fdebugoverlay%2Fmaven-metadata.xml&label=maven-central-snapshots&color=brightgreen)](https://central.sonatype.com/artifact/com.ms-square/debugoverlay)
[![API 24+](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://developer.android.com/tools/releases/platforms#7.0)
[![License](https://img.shields.io/badge/license-Apache%202-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0)

**Zero-configuration runtime diagnostics for debug/internal builds—available instantly when you need it.**

DebugOverlay gives you a lightweight, always-available look into your app's runtime state so you can spot regressions *before* you reach for heavy profilers. Use it during development, QA testing, CI runs, or customer repro investigations.

**What makes it different:**
- **Proactive** – Catch issues while developing, not after a QA report
- **Self-contained** – No companion app, no adb, no cloud account
- **No special permissions** – No `SYSTEM_ALERT_WINDOW` required
- **Developer-first** – Low-friction access to runtime state

> **Not a replacement for:** Deep profiling (Android Profiler), leak detection (LeakCanary), or crash reporting (Crashlytics). DebugOverlay is your **"check engine light"**—it tells you *when* to look deeper.

## Quick Start

```kotlin
// 1. Add snapshot repository (settings.gradle.kts → repositories block)
maven(url = "https://central.sonatype.com/repository/maven-snapshots")

// 2. Add dependency (app/build.gradle.kts)
debugImplementation("com.ms-square:debugoverlay:2.0.0-SNAPSHOT")

// That's it! Overlay appears automatically on app launch.
// Tap to open debug panel. Long-press to drag.
```

See [Installation](#installation) for full `settings.gradle.kts` context.

<img src="art/readme_simple_demo.gif" alt="DebugOverlay Demo">

## Features

### Overlay Metrics
Draggable overlay with real-time metrics and sparklines:
- **CPU** – App CPU usage from `/proc/self/stat`
- **Heap** – JVM heap usage percentage
- **PSS** – Proportional Set Size in MB
- **FPS** – Real-time frame rate

### Debug Panel
Tap the overlay to open a full-screen diagnostic panel:

- **Logcat** – Live system logcat stream with level filtering and search
- **[Custom Log]** – Additional tab when using [Timber](#timber-log-capture) or custom log trackers
- **AppExits** – App exit (e.g., Crash/ANR) history on Android 11+ with stack traces
- **Network** – Request list with timing and inspection ([setup required](#network-request-tracking))
- **JankStats** – Frame timing analysis and jank breakdown
- **UI** – View hierarchy via [Radiography](https://github.com/square/radiography)
- **Device** – Hardware specs, OS info, battery, network status
- **Bug Report** – One-tap HTML report with screenshot and diagnostics

<img src="art/readme_debug_panel.gif" alt="Debug Panel">

## Installation

**Requirements:** Android 7.0+ (API 24). Pure Java/XML apps work fine—no Compose setup needed in your app.

### 1. Add snapshot repository

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    maven(url = "https://central.sonatype.com/repository/maven-snapshots")
  }
}
```

> Stable release planned. Remove this repository once 2.0.0 is released on Maven Central.

### 2. Add dependency

```kotlin
dependencies {
  debugImplementation("com.ms-square:debugoverlay:2.0.0-SNAPSHOT")
}
```

## Usage

### Auto-install

The overlay installs automatically via AndroidX Startup. No code required—just add the dependency.

To disable auto-install (e.g., for specific build flavors), add this to your `AndroidManifest.xml`:
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
  <application>
    <provider
      android:name="androidx.startup.InitializationProvider"
      android:authorities="${applicationId}.androidx-startup"
      tools:node="merge">
      <meta-data
        android:name="com.ms.square.debugoverlay.DebugOverlayStartupInitializer"
        tools:node="remove" />
    </provider>
  </application>
</manifest>
```

> **Note:** Manual lifecycle control is not supported in v2.0.0 at the moment. Disabling auto-install means the overlay won't appear.

### Overlay modes

By default DebugOverlay shows the real-time metrics overlay (`OverlayMode.FullMetrics`). For QA/internal builds, you can show only the Bug Reporter FAB:

```kotlin
class MyApp : Application() {
  override fun onCreate() {
    super.onCreate()

    DebugOverlay.configure {
      copy(overlayMode = OverlayMode.BugReporterOnly)
    }
  }
}
```

<img src="art/readme_bug_reporter_only.gif" alt="Bug Reporter Only Mode">

### Network request tracking

```kotlin
dependencies {
  debugImplementation("com.ms-square:debugoverlay:2.0.0-SNAPSHOT")
  debugImplementation("com.ms-square:debugoverlay-extension-okhttp:2.0.0-SNAPSHOT")
}
```

```kotlin
val client = OkHttpClient.Builder()
  .addNetworkInterceptor(DebugOverlayNetworkInterceptor())
  .build()
```

By default it redacts common auth headers and query params. To customize redaction and body size limits:

```kotlin
import com.ms.square.debugoverlay.extension.okhttp.DEFAULT_HEADERS_REDACT
import com.ms.square.debugoverlay.extension.okhttp.DEFAULT_QUERY_PARAMS_REDACT

val client = OkHttpClient.Builder()
  .addNetworkInterceptor(
    DebugOverlayNetworkInterceptor(
      headersNameToRedact = DEFAULT_HEADERS_REDACT + setOf("x-my-custom-token"),
      queryParamsNameToRedact = DEFAULT_QUERY_PARAMS_REDACT + setOf("sessionId"),
      maxBodySize = 128 * 1024L, // 128KB (use 0L to omit all bodies)
    )
  )
  .build()
```

### Timber log capture

```kotlin
dependencies {
  debugImplementation("com.ms-square:debugoverlay:2.0.0-SNAPSHOT")
  debugImplementation("com.ms-square:debugoverlay-extension-timber:2.0.0-SNAPSHOT")
}
```

Auto-plants via AndroidX Startup. Adds a separate "Timber" tab alongside Logcat with full stack traces.

To disable auto-plant, remove `TimberTreeStartupInitializer` via manifest merger:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
  <application>
    <provider
      android:name="androidx.startup.InitializationProvider"
      android:authorities="${applicationId}.androidx-startup"
      tools:node="merge">
      <meta-data
        android:name="com.ms.square.debugoverlay.extension.timber.TimberTreeStartupInitializer"
        tools:node="remove" />
    </provider>
  </application>
</manifest>
```

Then call `Timber.plant(DebugOverlayTimberTree())` manually.

### Bug Reporting

In `OverlayMode.FullMetrics`, tap the bug icon in the debug panel toolbar. In `OverlayMode.BugReporterOnly`, tap the bug reporter FAB. Generates a ZIP with:
- Interactive HTML dashboard
- Embedded screenshot
- Logs (Logcat/Timber)
- Network history (requires OkHttp extension)
- JankStats
- App exit history (Android 11+)
- UI hierarchy
- Device info

If you dismiss the metadata dialog instead of submitting, DebugOverlay saves the capture as a **draft** (stored in app cache). Next time you open Bug Report, you can resume or delete saved drafts.

> **Privacy:** Reports contain raw logs and network data. Review before sharing externally. The OkHttp extension supports header redaction and body size limits to minimize sensitive data capture.

<img src="art/readme_bug_report_demo.gif" alt="Sample Bug Report">
<img src="art/readme_bug_report_drafts.png" alt="Bug Report Draft Picker">

## Advanced Setup

### Release builds with overlay

Test R8-optimized builds while keeping diagnostics:

```kotlin
android {
  buildTypes {
    create("releaseWithOverlay") {
      initWith(getByName("release"))
      matchingFallbacks += "release"
      applicationIdSuffix = ".internal"
    }
  }
}

dependencies {
  "releaseWithOverlayImplementation"("com.ms-square:debugoverlay:2.0.0-SNAPSHOT")
}
```

```bash
./gradlew installReleaseWithOverlay
```

## Known Limitations

- Data is stored locally only —> no cloud sync or team collaboration features

## Sample App

```shell
./gradlew :sample:assembleDebug
```

<img src="art/readme_sample_app_demo.gif" alt="Sample App Demo">

## License

[Apache License 2.0](LICENSE)
