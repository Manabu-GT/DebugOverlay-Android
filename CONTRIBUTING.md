# Contributing to DebugOverlay

Thank you for considering contributing to DebugOverlay! This document outlines how to contribute effectively.

## Code of Conduct

- Be respectful and constructive in discussions
- Focus on the problem, not the person
- Welcome newcomers and help them get started

## Getting Started

### Prerequisites

- Android Studio Narwhal (2025.1.1) or later
- JDK 21+
- Kotlin 2.2+
- Basic knowledge of Jetpack Compose

### Setup

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/DebugOverlay-Android.git
   cd DebugOverlay-Android
   ```
3. Add the upstream remote:
   ```bash
   git remote add upstream https://github.com/Manabu-GT/DebugOverlay-Android.git
   ```
4. Open the project in Android Studio and sync Gradle

## How to Contribute

### Reporting Bugs

Before creating an issue:
1. Search [existing issues](https://github.com/Manabu-GT/DebugOverlay-Android/issues) to avoid duplicates
2. Check [GitHub Discussions](https://github.com/Manabu-GT/DebugOverlay-Android/discussions) for known problems

When ready, [file a bug report](https://github.com/Manabu-GT/DebugOverlay-Android/issues/new?template=bug_report.md).

### Suggesting Features

Open a [feature request issue](https://github.com/Manabu-GT/DebugOverlay-Android/issues/new?template=feature_request.md).

### Pull Requests

1. **Create an issue first** for significant changes to discuss the approach
2. **Branch from `main`** using the naming conventions below
3. **Keep changes focused** — one feature or fix per PR
4. **Write tests** for new functionality (see [Testing](docs/TESTING.md))
5. **Update documentation** if needed
6. **Run checks** before submitting:
   ```bash
   ./gradlew spotlessApply  # Format code (must run before check)
   ./gradlew check          # Run all checks (tests, lint, detekt, spotless)
   ```

## Branch Naming

| Prefix | Use Case |
|--------|----------|
| `feature/` | New features |
| `fix/` | Bug fixes |
| `docs/` | Documentation only |
| `refactor/` | Code restructuring |
| `test/` | Test additions |

Example: `feature/add-memory-tracking`, `fix/overlay-crash-on-rotate`

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```text
type(scope): description

[optional body]
```

**Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

**Scopes:** `core`, `overlay`, `okhttp`, `timber`, `sample`, `build`

Examples:
```text
feat(core): add CPU usage tracking module
fix(overlay): prevent crash when activity is destroyed
docs(readme): update installation instructions
```

## Code Style

See [Coding Conventions](docs/CODING.md) for detailed guidelines. Also see [Build & Gradle](docs/BUILD.md) for toolchain and module conventions.

Key points:
- All new code in Kotlin
- 4-space indentation
- Run `./gradlew spotlessApply` before committing
- Use `runCatchingNonCancellation` in suspend functions

## Testing

See [Testing](docs/TESTING.md) for detailed guidelines. For code review standards, see [Code Review](docs/REVIEW.md).

```bash
# All checks
./gradlew check

# Specific module
./gradlew :debugoverlay-core:check
```

## Project Structure

| Module | Description |
|--------|-------------|
| `debugoverlay-core` | Compose-based runtime and shared components |
| `debugoverlay` | Primary public API |
| `debugoverlay-extension-okhttp` | OkHttp interceptor for network tracking |
| `debugoverlay-extension-timber` | Timber tree for log capture |
| `sample` | Demo application |

## Questions?

- Open a [GitHub Discussion](https://github.com/Manabu-GT/DebugOverlay-Android/discussions) for questions
- Check [existing issues](https://github.com/Manabu-GT/DebugOverlay-Android/issues) for known problems

Thank you for contributing!
