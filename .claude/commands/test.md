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

## Behavior

- Creates new test files in `src/test/kotlin/` mirroring the source path structure
- If a test file already exists, prompts before modifying (use `--force` to skip prompt)
- Generates tests for public functions and classes (skips private implementation details)
- If the source file has compilation errors, reports the error and skips test generation
- Detects existing test coverage by reading the test file and skips methods that already have tests

## Usage

- Test a specific file: `/test <file-path>`
- Test all changed .kt files: `/test --changed`

## Examples

```bash
/test debugoverlay-core/src/main/kotlin/com/ms/square/debugoverlay/internal/data/source/MemoryDataSource.kt
/test --changed
```

## Test Generation Rules

1. **Match existing patterns**: Read existing tests in the module to match naming and structure. If no tests exist in the module, follow patterns from `docs/TESTING.md` and check other modules for common patterns.
2. **Use project tech stack**: JUnit 4, Truth assertions, MockK, Turbine, Robolectric
3. **Follow DO/DON'T Mock guidance** from `docs/TESTING.md`
4. **Test file location**: `src/test/kotlin/` mirroring main source path
5. **Naming**: Use backticks for test names (e.g., `` `returns null when empty` ``)
6. **Test coverage scope**:
  - ✅ Generate tests for: ViewModels with state logic, Repositories with data transformations, utility functions with business logic, custom data structures (e.g., `EvictingQueue`)
  - ❌ Skip: Data classes with only properties, simple delegation patterns, one-line getters/setters, pure pass-through functions

## Quick Reference

For detailed patterns, see `docs/TESTING.md`:
- **Flows**: "Testing Flows" — when to use `.first()`, `.single()`, or Turbine
- **Virtual time**: "Testing with Virtual Time" — dispatcher injection
- **Test doubles**: "Mocking & Test Doubles" — fakes vs mocks
- **Context**: "Robolectric" — when to add `@RunWith`

## When using `--changed`

- Compare current branch against `main` (use `--base <branch>` to override)
- Only process `.kt` files in `src/main/` directories (exclude `src/test/`, `src/androidTest/`)
- Generate tests for files that don't have a corresponding test file at `src/test/kotlin/<package>/<ClassName>Test.kt`
- Skip files where a test file already exists (use `--force` to regenerate)

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
