DebugOverlay-Android
====================
[![Maven Central](https://img.shields.io/maven-metadata/v.svg?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fcom%2Fms-square%2Fdebugoverlay%2Fmaven-metadata.xml&label=maven-central-snapshots&color=brightgreen)](https://central.sonatype.com/artifact/com.ms-square/debugoverlay)
[![API 24+](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://developer.android.com/tools/releases/platforms#7.0)
[![License](https://img.shields.io/badge/license-Apache%202-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0)

**DebugOverlay** is an overlay fully written in Kotlin that runs inside your app process and surfaces real-time CPU, heap, PSS, and FPS metrics in a draggable UI. v2.0.0 rewrites the original 1.x Java implementation and no longer requires the `SYSTEM_ALERT_WINDOW` permission.

The overlay is ideal for debug builds, QA drops, or instrumentation test sessions where you want lightweight insight into runtime health without attaching profilers.

<img src="art/readme_simple_demo.gif" width="50%" alt="DebugOverlay Simple Demo">

## Highlights in v2.0.0-SNAPSHOT

- Pure Kotlin + Jetpack Compose implementation rendered using application-layer windows attached to each Activity (no system permissions)
- Updated metrics pipeline that continuously samples CPU, heap, process PSS, and frame rate with historical sparklines
- Drag-to-move UI with edge snapping, dark/light theme awareness, and shared position across activities
- Automatic install by default through a lightweight ContentProvider (or AndroidX Startup if you prefer)
- Minimum SDK 24 / target SDK 36

### Upcoming features
- Bottom sheet panel (tapping the overlay will reveal):
  - Logcat messages
  - Network usage statistics
  - Additional diagnostic data (TBD)

## Requirements

- Android 7.0 (API level 24) or higher

The library itself is implemented with Kotlin + Compose but ships as a regular AAR. You do **not** need to enable the Compose compiler plugin or migrate your app to Kotlin—pure Java/XML apps can consume the dependency via `debugImplementation`.

DebugOverlay is intended for debug builds in general; keep it out of release variants by using `debugImplementation` (shown below).

## Installation

### 1. Repositories

`2.0.0-SNAPSHOT` is published to Sonatype snapshots while I prep a stable release (target: early-Dec 2025). Add the repository next to `mavenCentral()`:

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

## What the overlay shows

- **CPU** – App CPU usage sampled from `/proc/self/stat` every second with a 16-sample historical sparkline
- **Heap** – JVM heap usage percentage relative to the max heap size, refreshed once per second
- **PSS** – Process Proportional Set Size in MB with automatic scaling based on observed max, sampled every 3 seconds to smooth out noise
- **FPS** – Current frame rate vs. the target frame rate, refreshed every second

Each row shows a status dot (green = healthy, yellow = warning, red = critical) that reacts to the current value. All metrics refresh at the cadence described above, and the overlay adapts to dark/light themes automatically. The panel can be long-pressed to drag; it snaps to the nearest horizontal edge when released and remembers its position across activities.

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
