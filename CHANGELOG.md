# Change Log

## Version 2.6.2 *(2026-07-21)*

### New Features

* **Formatted JSON bodies and full request/response view in HTML reports** – Request/response bodies in the HTML bug report are now pretty-printed when detected as JSON (via Content-Type header or content sniffing). Truncated bodies (>2KB) get a toggle to view more: "View Full" if the body fits within a 64KB cap, or "View More (first 64.0 KB of Y)" otherwise, capped per-body to bound report memory. Resolves [#258](https://github.com/Manabu-GT/DebugOverlay-Android/issues/258).

### Bug Fixes

* **Debug Panel filter bar now collapses on short-height windows** – The panel gains compact-height support, including height-aware scrolling for the top app bar. Log filtering switches to a responsive filter bar (expanded chips vs. compact dropdown) with improved selection indicators/semantics, and the Network tab renders a compact, pinned statistics header on short-height windows. Resolves [#257](https://github.com/Manabu-GT/DebugOverlay-Android/issues/257).

## Version 2.6.1 *(2026-05-21)*

### Bug Fixes

* **Speculative fix for "Create Report" being unresponsive on Xiaomi / MIUI** – `BugReportActivity` is no longer launched with `FLAG_ACTIVITY_NEW_TASK` when invoked from an Activity context; the flag is preserved for the FAB's non-Activity overlay context where it is still required. The combination of `NEW_TASK` + a transparent destination is suspected of tripping MIUI's pop-up window gate, but the symptom reported in [#251](https://github.com/Manabu-GT/DebugOverlay-Android/issues/251) could not be reproduced on the maintainer's devices, so this is an exploratory change rather than a verified fix.

## Version 2.6.0 *(2026-05-18)*

### New Features

* **Thermal status row** – Optional thermal-status indicator for the compact overlay, opt-in via `OverlayMode.FullMetrics(showThermal = true)`. Combines `PowerManager.getCurrentThermalStatus()` with `getThermalHeadroom()` per [Google's ADPF guidance](https://developer.android.com/games/optimize/adpf/thermal#device-limitations-of-the-thermal-api) so devices with an incomplete thermal HAL still surface a useful signal. Requires Android 11 (API 30) or above; row stays hidden on older devices. Resolves [#246](https://github.com/Manabu-GT/DebugOverlay-Android/issues/246).
* **Configurable Logcat buffer size** – New `Config.maxLogcatEntries` controls how many entries the built-in Logcat tab retains (default 300). Also bounds `logcat -T N` / `-t N` so it caps OS replay on panel open and on bug-report snapshots. Reassignable at runtime via `DebugOverlay.configure { maxLogcatEntries = … }`.

## Version 2.5.0 *(2026-05-12)*

### New Features

* **Clear logs from the debug panel** – New `ClearAll` toolbar button wipes accumulated Logcat / Timber / network entries mid-session so the next bug report covers only the relevant repro window. Built-in sources implement the new public `Clearable` interface; custom `LogSource` / `NetworkRequestSource` implementations can opt in by also implementing `Clearable`. Resolves [#236](https://github.com/Manabu-GT/DebugOverlay-Android/issues/236).

## Version 2.4.0 *(2026-05-07)*

### New Features

* **Shake-to-open extension** – New artifact `com.ms-square:debugoverlay-extension-trigger-shake`. Adding the dependency auto-installs (via AndroidX Startup) a foreground-only shake listener that calls `DebugOverlay.openPanel()`.

### Changes

* **`DebugPanelActivity` is now `launchMode="singleTop"`** – Repeat `DebugOverlay.openPanel()` calls while the panel is the top activity now reuse the existing instance via `onNewIntent` instead of stacking duplicates. When the panel is in the task but not on top (e.g., `BugReportActivity` is open), a fresh panel is pushed on top — the underlying activity is preserved.

## Version 2.3.0 *(2026-04-30)*

### New Features

* **Programmatic panel access** – New `OverlayMode.Hidden(customTabs)` suppresses the on-screen overlay entirely. New `DebugOverlay.openPanel(context)` launches the debug panel from any mode — wire it to your own debug menu, notification action, or gesture handler. Resolves [#228](https://github.com/Manabu-GT/DebugOverlay-Android/issues/228).

### Source-compat notes

* Adding `OverlayMode.Hidden` is source-breaking for downstream code that exhaustively `when`s on `OverlayMode`. Add a branch for `Hidden`.

## Version 2.2.1 *(2026-04-13)*

### Bug Fixes

* **Network tab** – Query parameters are now displayed in both the request list and the URL section of the request detail screen https://github.com/Manabu-GT/DebugOverlay-Android/pull/223

## Version 2.2.0 *(2026-03-29)*

### New Features

* **Custom Tab API** – Add app-specific tabs to the debug panel via `DebugTab` class. Custom tabs are appended after built-in tabs and configured via `OverlayMode.FullMetrics(customTabs = ...)`.

### Breaking Changes

* `OverlayMode.FullMetrics` changed from `data object` to `data class`. If you reference it explicitly, change `OverlayMode.FullMetrics` to `OverlayMode.FullMetrics()`. Zero-config users are unaffected.

### Build

* Upgrade to AGP 9.0.1 and Gradle 9.2.1
* Remove BCV plugin (incompatible with AGP 9's built-in Kotlin support — [BCV #312](https://github.com/Kotlin/binary-compatibility-validator/issues/312))

## Version 2.1.1 *(2026-03-26)*

### Dependencies

* Update AndroidX libraries (Core 1.17.0, Activity Compose 1.12.4, Lifecycle 2.10.0)
* Update kotlinx-serialization to 1.10.0
* Update Radiography to 2.9

## Version 2.1.0 *(2026-02-02)*

### New Features

* **Custom Bug Report Exporter** – Implement `BugReportExporter` to send bug reports directly to Jira, Slack, or other systems instead of using the default share sheet (#180)
  * New public models: `BugReport`, `BugReportSummary`, `BugReportArchive`, `ExportResult`
  * Drafts list now shows a "Shared" badge for submitted reports

### Dependencies

* Update AndroidX libraries
* Update Material library to 1.13.0

## Version 2.0.0 *(2026-01-26)*

**Complete rewrite.** DebugOverlay v2.0.0 gives you zero-config runtime diagnostics for debug builds. Catch performance issues and bugs during development—without heavy profilers or cloud dependencies. Built from the ground up with Jetpack Compose, Material Design 3, and modern Android APIs.

⚠️ This is not a drop-in upgrade from v1.x—the API surface and capabilities have changed significantly.

### New Features

* **Real-time Overlay** – Draggable overlay with live sparklines for CPU, Heap, PSS, and FPS

* **Debug Panel** – Tap the overlay to open a full-screen diagnostic panel with multiple tabs:
  * **Logcat** – Live logcat stream with level filtering and search
  * **App Exits** – Exit history (crash, ANR, etc.) with stack traces (API 30+)
  * **Network** – Request/response inspection with headers and bodies (requires OkHttp extension)
  * **JankStats** – Frame timing analysis powered by AndroidX Metrics
  * **UI Hierarchy** – View hierarchy inspection via Square's Radiography
  * **Device** – Hardware specs, OS info, battery, network status

* **Bug Reporter** – One-tap bug reports with:
  * Screenshot capture
  * Interactive HTML dashboard (self-contained, viewable offline)
  * All diagnostic data bundled in a shareable ZIP
  * Draft management – save incomplete reports and resume later
  * Custom data contributors – add app-specific files to reports

* **Overlay Modes** – Choose between `FullMetrics` (real-time overlay + debug panel) or `BugReporterOnly` (minimal FAB for QA/internal builds)

* **Timber Extension** – Separate "Timber" tab alongside Logcat with full stack traces (auto-registering via AndroidX Startup)

* **OkHttp Extension** – Network request tracking with configurable header redaction and body size limits

### Changes from v1.x

* **Built with Jetpack Compose** – Modern UI toolkit, no more custom Views
* **AndroidX Startup** – Zero-code initialization (just add the dependency)
* **No `SYSTEM_ALERT_WINDOW`** – Overlay renders within app window, no special permission needed
* **Minimum SDK is 24** (lowered from 26 in v1.1.4; original v1.0 was 14)
* **Modules removed** – `CpuFreqModule`, `NetStatsModule`, and custom module system replaced by built-in features
* **New artifact coordinates** – Extensions are now separate artifacts (`debugoverlay-extension-okhttp`, `debugoverlay-extension-timber`)

## Version 1.1.4 *(2025-10-27)*

> ⚠️ **Deprecated:** v1.x is no longer maintained. Please upgrade to [v2.0.0](#version-200-2026-01-26) for new features, bug fixes, and continued support.

* **Build & toolchain** – Migrated the entire project to Gradle Kotlin DSL with a shared `libs.versions.toml`, upgraded the wrapper/AGP stack (Gradle 8.13, AGP 8.13.0).
* **Android platform updates** – Raised `minSdk` to 26 / `targetSdk` to 36, migrated dependencies to AndroidX, and cleaned up manifest/service initialization (DebugOverlay now installs only in the app's main process and drops stale permission checks).
* **CPU/FPS modules** – Fixed CPU usage/frequency collectors to operate correctly on API 26+ and tidied related overlays/resources.

## Version 1.1.3 *(2017-09-24)*

* Add a NetStatsModule as an extension module
* Support Library 26.0.1 -> 26.1.0

## Version 1.1.2 *(2017-09-09)*

* Add a TimberModule as an extension module
* Update the LogcatLine to make it work with the TimberModule

## Version 1.1.1 *(2017-09-02)*

* Fix to close filereaders in CpuFreqDataModule after reading data

## Version 1.1.0 *(2017-09-01)*

* Android O support (Note: CpuUsageModule/CpuFreqModule will not work on Android O and above)
* Add a CpuFreqModule to show current/max frequencies of all the cpu cores
* Support Library 25.3.1 -> 26.0.1

## Version 1.0.1 *(2017-04-05)*

* Use split v4 support library as dependencies for less size
* Support Library 25.3.0 -> 25.3.1

## Version 1.0.0 *(2017-04-04)*

Initial release.
