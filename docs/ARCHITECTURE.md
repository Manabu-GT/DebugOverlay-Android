# Architecture Overview

This document describes the high-level architecture of DebugOverlay. For code locations and common tasks, see [AGENTS.md](../AGENTS.md).

## Module Structure

```
┌─────────────────┐     ┌────────────────────┐     ┌────────────────────┐
│   debugoverlay  │     │ extension-okhttp   │     │ extension-timber   │
│   (auto-start)  │     │ (OkHttp intercept) │     │ (Timber capture)   │
└────────┬────────┘     └─────────┬──────────┘     └─────────┬──────────┘
         │                        │                          │
         └────────────────────────┼──────────────────────────┘
                                  │
                                  ▼
                      ┌──────────────────────┐
                      │   debugoverlay-core  │
                      │                      │
                      │  • Data collection   │
                      │  • UI components     │
                      │  • Bug reporting     │
                      │  • Public interfaces │
                      └──────────────────────┘
```

| Module | Purpose |
|--------|---------|
| `debugoverlay-core` | All core functionality: metrics, UI, bug reports, extension interfaces |
| `debugoverlay` | Auto-installation via AndroidX Startup |
| `debugoverlay-extension-okhttp` | HTTP traffic capture for OkHttp clients |
| `debugoverlay-extension-timber` | Timber log capture with auto-plant |

## Data Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        Data Sources                             │
│  (LogcatDataSource, DeviceInfoDataSource, JankStatsDataSource)  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
             ┌─────────────────────────┐
             │ DebugOverlayDataRepository │
             │      (Central Hub)       │
             │                          │
             │   Aggregates all data    │
             │   Exposes as Flows       │
             └────────────┬─────────────┘
                          │
                          ▼
             ┌───────────────────────────────────┐
             │         Compose UI Layer         │
             │                                   │
             │  collectAsStateWithLifecycle()   │
             └───────────────────────────────────┘
```

**Key components:**
- `DebugOverlayDataRepository` — Central hub aggregating all data sources
- `OverlayViewManager` — Window attachment via Curtains, overlay lifecycle
- `BugReportGenerator` — Orchestrates capture → preview → ZIP flow

## Extension Model

Extensions integrate via interfaces in `debugoverlay-core`:

```kotlin
interface LogSource {
    val sourceName: String
    val logs: Flow<List<LogEntry>>
}

interface NetworkRequestSource {
    val requests: Flow<List<NetworkRequest>>
}

interface BugReportDataContributor {
    val filename: String
    fun writeTo(outputStream: OutputStream)
}
```

**Auto-registration pattern:** Extensions self-register in their `init` block by calling `DebugOverlay.configure {}`. AndroidX Startup provides zero-config initialization for core and Timber extension (via manifest-declared initializers); OkHttp extension requires manual interceptor registration.

## UI Architecture

```
DraggableOverlayPanel (attached via WindowManager)
├── FullMetrics mode → Compact metrics panel
│   └── Tap → DebugPanelDialog (tabbed interface)
└── BugReporterOnly mode → DraggableBugReporterFab
```

The overlay uses a synthetic `OverlayLifecycleOwner` to provide Compose lifecycle APIs outside the activity hierarchy. Lifecycle mirrors the target activity's lifecycle events (onResume, onPause, etc.)

## Architectural Invariants

| Invariant | Rationale |
|-----------|-----------|
| **No SYSTEM_ALERT_WINDOW** | Uses Curtains to attach to app windows; no special permission needed |
| **Bounded collections** | `EvictingQueue` prevents OOM from unbounded log/request accumulation |
| **Main process only** | Overlay is per-process singleton; no multi-process complexity |
| **Extensions depend on core** | Loose coupling via interfaces; core has no knowledge of extensions |

## Key Architectural Decisions

| Decision | Rationale                                                              |
|----------|------------------------------------------------------------------------|
| **AndroidX Startup** | Zero configuration for consumers; runs before `Application.onCreate()` |
| **Flow-based reactive data** | Natural fit for Compose; lifecycle-aware collection                    |
| **Curtains for window management** | Robust overlay attachment without system permissions                   |
| **Extension pattern** | Extensions self-register with core for convenience                     |
| **Synthetic LifecycleOwner** | Enables Compose lifecycle APIs for overlay outside activity hierarchy  |
| **PixelCopy for screenshots** | Hardware-accelerated capture (API 26+); Canvas fallback for older      |
| **No-backup storage for drafts** | Bug report drafts persist across launches but don't sync to cloud      |

## Third-Party Dependencies

| Library | Purpose |
|---------|---------|
| [Curtains](https://github.com/square/curtains) | Window attachment and activity tracking |
| [Radiography](https://github.com/square/radiography) | View hierarchy inspection |
| [KotlinX Serialization](https://github.com/Kotlin/kotlinx.serialization) | JSON serialization for bug reports |

*See `gradle/libs.versions.toml` for current versions.*
