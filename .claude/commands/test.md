---
name: test
description: Generate focused unit tests for Kotlin files using project testing guidelines
---

Generate unit tests for the specified file(s) following project conventions.

## Process

1. **Read testing guidelines** from `docs/TESTING.md`
2. **Analyze the target code** to understand its purpose and dependencies
3. **Detect component type** (ViewModel, Repository, data source, utility, etc.)
4. **Generate focused tests** following project patterns

## Usage

- Test a specific file: `/test <file-path>`
- Test all changed .kt files: `/test --changed`

## Examples

```
/test debugoverlay-core/src/main/kotlin/com/ms/square/debugoverlay/internal/data/source/MemoryDataSource.kt
/test --changed
```

## Test Generation Rules

1. **Read existing tests** in the module to match naming and structure patterns
2. **Use project tech stack**: JUnit 4, Truth assertions, MockK, Turbine, Robolectric
3. **Follow DO/DON'T Mock guidance** from `docs/TESTING.md`
4. **Test file location**: `src/test/kotlin/` mirroring main source path
5. **Naming**: Use backticks for test names (e.g., `` `returns null when empty` ``)
6. **Skip trivial code**: Don't test simple getters, data classes, or framework behavior

## Quick Reference

For detailed patterns, see `docs/TESTING.md`:
- **Flows**: "Testing Flows" — when to use `.first()`, `.single()`, or Turbine
- **Virtual time**: "Testing with Virtual Time" — dispatcher injection
- **Test doubles**: "Mocking & Test Doubles" — fakes vs mocks
- **Context**: "Robolectric" — when to add `@RunWith`

## When using `--changed`

- Compare current branch against `main`
- Only process `.kt` files (exclude test files)
- Generate tests for each file that doesn't have corresponding test coverage

## Running Coverage

```bash
# All modules
./gradlew mergedJacocoReport

# Single module
./gradlew :debugoverlay-core:jacocoTestReport
```

Reports:
- Merged: `build/reports/jacoco/mergedJacocoReport/html/index.html`
- Module: `<module>/build/reports/jacoco/jacocoTestReport/html/index.html`
