# Testing

## Commands by Change Type

| Change Type | Command |
|-------------|---------|
| Gradle/build logic | `./gradlew help` (then smallest assemble task if needed) |
| Core runtime | `./gradlew :debugoverlay-core:check` |
| Main library | `./gradlew :debugoverlay:check` |
| OkHttp extension | `./gradlew :debugoverlay-extension-okhttp:check` |
| Timber extension | `./gradlew :debugoverlay-extension-timber:check` |
| Sample app | `./gradlew :sample:assembleDebug` |
| Documentation only       | No build/test required - verify links and accuracy |

## What to Test

**DO test** (project's own logic):
- Business logic: calculations, transformations, aggregations
- Edge case handling in your code
- State management and data flow
- Error handling and fallback behavior
- Custom data structures (e.g., `EvictingQueue`)
- Integration between your own components

**DO NOT test** (framework/library behavior):
- Kotlin stdlib behavior (e.g., that `forEach` iterates)
- AndroidX/Jetpack internals (e.g., `LifecycleRegistry` transitions)
- Third-party library internals (e.g., OkHttp request building)
- Android framework behavior (e.g., `StateFlow.update` atomicity)

**Rule of thumb:** If you're verifying that a framework API does what its documentation says, you're testing the framework, not your code.

## Avoiding Duplicate Tests

Duplicate tests waste CI time and create maintenance burden when behavior changes require updating multiple tests.

Before writing a new test, verify it tests a **unique behavior**:

1. **Check existing tests** — Search for tests covering the same method/class and review their scenarios
2. **Distinct behavior per test** — Each test should verify a different behavior, edge case, or failure mode
3. **Parameterize when appropriate** — For the same logic with many input variations, use parameterized tests

```kotlin
// DUPLICATE: Same scenario, different names (1s = 1000ms)
@Test fun `returns FPS when interval elapses`()           // 60 frames at 1s
@Test fun `handles elapsed time exactly at boundary`()   // 60 frames at 1000ms ← SAME TEST

// DISTINCT: Different behaviors
@Test fun `returns FPS when interval elapses`()  // Normal calculation
@Test fun `FPS capped at maxFps`()               // Capping branch
@Test fun `first frame returns null`()           // Early return path

// PARAMETERIZED: Same logic, multiple inputs
@ParameterizedTest
@ValueSource(strings = ["", "invalid", "test@", "@example.com"])
fun `rejects invalid email format`(email: String) {
    assertThat(validator.isValid(email)).isFalse()
}
```

## Test Naming

**Unit tests** (`src/test`) — use backticks:
```kotlin
@Test fun `escapeHtml returns same instance when no special characters`()
@Test fun `add returns null when queue is not at capacity`()
```

**Instrumented tests** (`src/androidTest`) — use underscores (backticks have issues on API < 30):
```kotlin
@Test fun escapeHtml_returnsNull_whenInputIsEmpty()
```

Guidelines:
- Start with the method/subject being tested
- Describe the behavior, not the implementation
- Include conditions when relevant ("when X", "with Y")

## Examples

```kotlin
// GOOD: Tests our calculation logic
@Test
fun `percentage calculated correctly for mixed inputs`() {
    repeat(7) { processor.process(createData(flag = false)) }
    repeat(3) { processor.process(createData(flag = true)) }
    assertThat(processor.state.value.percentage).isEqualTo(0.3f)
}

// BAD: Tests that Kotlin's forEach iterates correctly
@Test
fun `handles multiple items`() {
    // If this just verifies forEach works, it's testing Kotlin stdlib
}

// GOOD: Tests our interceptor's redaction logic
@Test
fun `redacts Authorization header`() {
    val result = interceptor.captureHeaders(mapOf("Authorization" to "Bearer secret"))
    assertThat(result["Authorization"]).isEqualTo("[REDACTED]")
}

// BAD: Tests that OkHttp MockWebServer returns responses
@Test
fun `server returns 200`() {
    server.enqueue(MockResponse().setResponseCode(200))
    // Just testing MockWebServer works, not our code
}
```
