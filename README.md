DebugOverlay-Android
====================
[![Maven Central](https://img.shields.io/maven-metadata/v.svg?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fcom%2Fms-square%2Fdebugoverlay%2Fmaven-metadata.xml&label=maven-central-snapshots&color=brightgreen)](https://central.sonatype.com/artifact/com.ms-square/debugoverlay)
[![API 24+](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://developer.android.com/tools/releases/platforms#7.0)
[![License](https://img.shields.io/badge/license-Apache%202-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0)

**DebugOverlay** gives you always-on visibility into CPU, memory, FPS, logs, network, and UI hierarchy—right inside your app, no permissions required.

Drop it into your debug build and get a draggable overlay with real-time metrics plus a full diagnostic panel. Ideal for development, QA testing, or instrumentation runs where you need runtime insight without attaching profilers.

> v2.0.0 is a complete Kotlin + Compose rewrite of the original 1.x Java implementation.

<img src="art/readme_simple_demo.gif" width="50%" alt="DebugOverlay Simple Demo">

## Features

### Overlay Metrics
The draggable overlay displays real-time metrics with 16-sample historical sparklines:

- **CPU** – App CPU usage sampled from `/proc/self/stat` every second
- **Heap** – JVM heap usage percentage relative to max heap, refreshed every second
- **PSS** – Process Proportional Set Size in MB, sampled every 3 seconds
- **FPS** – Current frame rate vs. target frame rate, refreshed every second

Each row shows a status dot (green/yellow/red) based on current health. Long-press to drag; the overlay snaps to the nearest edge and remembers its position across activities.

### Debug Panel
Tap the overlay to open a full-screen diagnostic panel with five tabs:

- **Logcat** – Live logcat stream with level filtering (V/D/I/W/E), search, and tap-to-expand details
- **Network** – Upload/download totals, request list with timing/size, and full request/response inspection (requires [interceptor setup](#network-request-tracking))
- **JankStats** – Frame timing analysis showing jank percentage, per-state breakdown, and individual janky frame details
- **UI** – View hierarchy powered by [Radiography](https://github.com/square/radiography) with refresh and copy-to-clipboard
- **Device** – Hardware specs, OS info, memory/storage usage, battery, and network status

<img src="art/readme_debug_panel.gif" width="50%" alt="Debug Panel">

### v2.0.0 Highlights
- Pure Kotlin + Jetpack Compose (no system permissions required)
- Automatic install via ContentProvider or AndroidX Startup
- Dark/light theme support
- Minimum SDK 24 / target SDK 36

### Upcoming Features
- **Bug reporting** – Export diagnostics and share bug reports
- **Custom tab API** – Allow apps to register custom diagnostic tabs

## Requirements

- Android 7.0 (API level 24) or higher

The library itself is implemented with Kotlin + Compose but ships as a regular AAR. You do **not** need to enable the Compose compiler plugin or migrate your app to Kotlin—pure Java/XML apps can consume the dependency via `debugImplementation`.

DebugOverlay is intended for debug builds in general; keep it out of release variants by using `debugImplementation` (shown below).

## Installation

### 1. Repositories

`2.0.0-SNAPSHOT` is published to Sonatype snapshots while I prep a stable release (target: late-Dec 2025). Add the repository next to `mavenCentral()`:

```kotlin
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven(url = "https://central.sonatype.com/repository/maven-snapshots")
  }
}
```

> **Note:** Snapshot builds are unstable. For production-critical testing, consider waiting for the stable release on Maven Central.

### 2. Pick the integration that fits your app

```kotlin
dependencies {
  // Option A: default auto-install via a lightweight ContentProvider
  debugImplementation("com.ms-square:debugoverlay:2.0.0-SNAPSHOT")

  // Option B: integrate with AndroidX Startup if you already use it
  // debugImplementation("com.ms-square:debugoverlay-androidx-startup:2.0.0-SNAPSHOT")
}
```

Use the same coordinate for instrumentation tests (e.g., `androidTestImplementation`) if you ever want overlays while running Espresso/UI Automator suites.

## Usage

### Auto-install (default `debugoverlay` artifact)

In debug builds the overlay installs itself on app startup via `DebugOverlayInstaller`, a ContentProvider that runs before your `Application`. The overlay only attaches in the main process and ignores secondary processes.

### AndroidX Startup integration

If your app already depends on `androidx.startup:startup-runtime`, include `debugoverlay-androidx-startup` instead of the default artifact. It registers `DebugOverlayStartupInitializer`, which automatically installs the overlay on app start. No additional configuration is required.

### Network request tracking

To see HTTP requests in the Network tab, add the OkHttp extension and attach the interceptor:

```kotlin
dependencies {
  debugImplementation("com.ms-square:debugoverlay-extension-okhttp:2.0.0-SNAPSHOT")
}
```

```kotlin
val client = OkHttpClient.Builder()
  .addNetworkInterceptor(DebugOverlayNetworkInterceptor(maxStoredRequests = 100))
  .build()
```

The interceptor captures request/response metadata (URL, method, status, timing, size) and displays it in the debug panel. Use `maxStoredRequests` to limit memory usage.

## Known Limitations

The debug overlay panel may appear below dialogs (AlertDialog, BottomSheetDialog, etc.)
due to Android window z-ordering. This is a trade-off to avoid requiring the
SYSTEM_ALERT_WINDOW permission. Will revisit this before the official v2.0.0 release, though.

## Sample app

`sample/` is a Jetpack Compose demo that consumes the Android Weekly feed and runs DebugOverlay during development. Build it with:

```shell
./gradlew :sample:assembleDebug
```

Use it as a reference for wiring the dependency and exercising the overlay while navigating multiple screens.

<img src="art/readme_sample_app_demo.gif" width="50%" alt="DebugOverlay Sample App Demo">

## License

DebugOverlay-Android is distributed under the [Apache License 2.0](LICENSE).
