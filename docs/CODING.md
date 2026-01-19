# Coding Conventions

## Language

All new code must be written in Kotlin.

## Coroutines

In suspend functions, **always use `runCatchingNonCancellation` instead of `runCatching`**.

- `runCatching` catches ALL exceptions including `CancellationException`, which breaks structured concurrency
- `runCatchingNonCancellation` (defined in `debugoverlay-core/src/main/kotlin/com/ms/square/debugoverlay/internal/util/Results.kt`) re-throws `CancellationException`
- Exception: In non-suspend contexts (regular functions, callbacks), `runCatching` is acceptable

## String Resources vs Hardcoded Strings

**Use String Resources for:**
- UI labels (Copy, Back, Refresh, section headers)
- Tab names
- Empty state messages ("No AppExit history", "No request headers")
- Accessibility descriptions (contentDescription)

**Hardcode is acceptable for:**
- Technical explanations (developer-facing diagnostic guidance)
- Enum labels that mirror Android API names (e.g., "ANR", "Low Memory")
- Code/API references and stack traces
- Process importance labels ("Foreground", "Cached")
- Emojis used as visual decorations
- Preview-only code

**Rule of thumb:** If QA/PM might see it and it reads like UI copy → StringRes. If it's technical content for developers → hardcode is acceptable.

## Formatting

- 4-space indentation for Kotlin/Java
- Run `./gradlew spotlessApply` after code changes
- Do not introduce sweeping style-only diffs
