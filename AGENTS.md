# DebugOverlay Agents Guide

A Jetpack Compose library that displays real-time debug information as an overlay on Android apps.

## Quick Reference

| Module | Description |
|--------|-------------|
| `debugoverlay-core` | Compose runtime and shared components |
| `debugoverlay` | Primary public API |
| `debugoverlay-extension-okhttp` | OkHttp network tracking |
| `debugoverlay-extension-timber` | Timber log capture |
| `sample` | Demo application |

## Build Commands

```bash
# Format code (required before check)
./gradlew spotlessApply

# Run all checks (tests, lint, detekt, spotless)
./gradlew check

# Build sample app
./gradlew :sample:assembleDebug
```

**Important:** Always run `spotlessApply` after code changes, before `check`.

## Output Format

- Reference code locations as `path/to/File.kt:42`
- Include line numbers when showing code snippets
- Structure responses: **Summary** → **Changes** → **Validation** → **Follow-ups**

## Detailed Guides

- [Coding Conventions](docs/CODING.md) — coroutines, string resources, formatting
- [Build & Gradle](docs/BUILD.md) — toolchain, modules, version catalog
- [Testing](docs/TESTING.md) — what to test, naming conventions, examples
- [Code Review](docs/REVIEW.md) — review protocol, analysis dimensions
- [Git Workflow](docs/GIT.md) — commit hygiene, GitHub CLI, planning
