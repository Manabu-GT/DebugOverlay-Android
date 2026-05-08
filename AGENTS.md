# DebugOverlay Agents Guide

A Jetpack Compose library that displays real-time debug information as an overlay on Android apps. See [Architecture](docs/ARCHITECTURE.md) for system design overview.

## Quick Reference

| Module | Description |
|--------|-------------|
| `debugoverlay-core` | Compose runtime and shared components |
| `debugoverlay` | Primary public API |
| `debugoverlay-extension-okhttp` | OkHttp network tracking |
| `debugoverlay-extension-timber` | Timber log capture |
| `debugoverlay-extension-trigger-shake` | Shake-to-open-panel trigger (auto-installs via Startup) |
| `sample` | Demo application |

## Code Map

**Key entry points:**
- `DebugOverlay` — `debugoverlay-core/src/main/kotlin/com/ms/square/debugoverlay/DebugOverlay.kt`
- `DebugOverlayDataRepository` — `debugoverlay-core/src/main/kotlin/com/ms/square/debugoverlay/internal/data/DebugOverlayDataRepository.kt`
- `OverlayViewManager` — `debugoverlay-core/src/main/kotlin/com/ms/square/debugoverlay/internal/OverlayViewManager.kt`
- `BugReportGenerator` — `debugoverlay-core/src/main/kotlin/com/ms/square/debugoverlay/internal/bugreport/BugReportGenerator.kt`

**Common tasks:**
| Task | Where to look |
|------|---------------|
| Add new data source | `internal/data/source/` — create new data source class, wire into `DebugOverlayDataRepository` |
| Add new debug panel tab | `internal/ui/DebugPanelDialog.kt` — add to `DebugTab` enum and `DebugPanelTabContent` |
| Modify overlay behavior | `OverlayViewManager.kt` — window attachment, lifecycle sync |
| Add bug report data | Implement `BugReportDataContributor` interface, register via `DebugOverlay.addBugReportContributor()` |
| Create new extension | Implement `LogSource` or `NetworkRequestSource`, self-register in `init` block |
| Modify bug report flow | `BugReportGenerator.kt` (orchestration), `BugReportDraftStorage.kt` (persistence) |
| Change metrics display | `internal/ui/DebugOverlayPanel.kt` (compact overlay), `DebugOverlayPanelDataSource.kt` (aggregation) |

## Build Commands

```bash
# Format code (required before check)
./gradlew spotlessApply

# Run all checks (tests, lint, detekt, spotless)
./gradlew check

# Build sample app
./gradlew :sample:assembleDebug
```

**Important:** Always run `spotlessApply` after making code changes and before running `check`.

## Planning

For non-trivial work, create `tools/ai/plans/PLAN_<TASK_NAME>.md`:
1. Draft proposed steps
2. Wait for maintainer approval
3. Update after each approved step

## Output Format

- Reference code locations as `path/to/File.kt:42`
- Include line numbers when showing code snippets
- Structure responses: **Summary** → **Changes** → **Validation** → **Follow-ups**

## Detailed Guides

- [Architecture](docs/ARCHITECTURE.md) — module structure, data flow, extension model, key decisions
- [Coding Conventions](docs/CODING.md) — coroutines, string resources, formatting
- [Build & Gradle](docs/BUILD.md) — toolchain, modules, version catalog
- [Testing](docs/TESTING.md) — what to test, naming conventions, examples
- [Code Review](docs/REVIEW.md) — review protocol, analysis dimensions
- [Git Workflow](docs/GIT.md) — commit hygiene, GitHub CLI
