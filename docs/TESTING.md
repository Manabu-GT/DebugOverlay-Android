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
- Coroutines library behavior (e.g., `StateFlow.update` atomicity)

**Rule of thumb:** If you're verifying that a framework API does what its documentation says, you're testing the framework, not your code.

## Avoiding Duplicate Tests

Duplicate tests waste CI time and create maintenance burden when behavior changes require updating multiple tests.

Before writing a new test, verify it tests a **unique behavior**:

1. **One test per behavior** — Group assertions verifying the same logical behavior; multiple assertions are fine when they support one behavior
2. **Test public API when sufficient** — If internal implementation is fully exercised through public API tests, skip separate internal tests. Test internal classes directly when they have edge cases unreachable through public API.
3. **Parameterize when appropriate** — For the same logic with many input variations, use parameterized tests

```kotlin
// BAD: Two tests for one behavior (defaults)
@Test fun `Config has FullMetrics as default overlayMode`()
@Test fun `Config has NoOpNetworkRequestSource as default`()

// GOOD: One test, multiple assertions for the same behavior
@Test fun `Config has correct defaults`() {
    val config = Config()
    assertThat(config.overlayMode).isEqualTo(OverlayMode.FullMetrics)
    assertThat(config.networkRequestSource).isEqualTo(NoOpNetworkRequestSource)
    assertThat(config.customLogSource).isNull()
}

// GOOD: Separate tests for distinct code paths
@Test fun `returns FPS when interval elapses`()  // Normal calculation
@Test fun `FPS capped at maxFps`()               // Capping branch

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

## Test Class Structure

Prefer `private val` over `lateinit var`. JUnit 4 creates a new test class instance per test method, so class-level `val` properties are already isolated. Avoid `@Before` when inline initialization works.

**When `@Before`/`@After` are appropriate:** External resources requiring lifecycle management (e.g., `MockWebServer.start()/shutdown()`).

## Testing Flows

| Scenario | Approach |
|----------|----------|
| Single emission (infinite Flow) | Use `.first()` — simpler than Turbine |
| Single emission (finite Flow) | Use `.single()` to also assert completion |
| Multiple emissions or timing | Use Turbine's `.test { }` with `awaitItem()` |
| Interval/timing verification | Inject `StandardTestDispatcher`, use `advanceTimeBy()` |

For infinite Flows tested with Turbine, always end with `cancelAndIgnoreRemainingEvents()` (infinite Flows won't complete naturally, so tests hang without explicit cancellation).

## Testing with Virtual Time

`Dispatchers.setMain()` only replaces `Dispatchers.Main` — it does **not** affect `Dispatchers.Default` or `Dispatchers.IO`.

**For code using only `Dispatchers.Main`:** Call `Dispatchers.setMain(testDispatcher)` before the test (in `@Before` or a `TestRule`). `runTest {}` will automatically use that scheduler—no need to pass the dispatcher explicitly.

**For code using `Dispatchers.Default` or `IO`:** You must inject them so tests can control virtual time and share a scheduler:
1. Accept dispatchers as constructor parameters with production defaults
2. Inject `StandardTestDispatcher` in tests
3. Ensure `runTest` uses the same scheduler (pass the dispatcher to `runTest(testDispatcher)` or build dispatchers with a shared `TestCoroutineScheduler`)
4. Use `advanceTimeBy()` to control time

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class IntervalFlowTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dataSource = MyDataSource(dispatcher = testDispatcher)

    @Test
    fun `emits at interval`() = runTest(testDispatcher) {
        dataSource.values(interval = 100.milliseconds).test {
            awaitItem() // first emission
            advanceTimeBy(100.milliseconds)
            awaitItem() // second emission
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

| Test Dispatcher | Behavior | When to Use |
|-----------------|----------|-------------|
| `StandardTestDispatcher` | Queues coroutines; use `advanceTimeBy()` or `advanceUntilIdle()` to progress | Timing-sensitive logic (delays, timeouts, debounce) |
| `UnconfinedTestDispatcher` | Eagerly executes coroutines until suspension; no `advanceTimeBy()` needed | Simple emission validation, non-timing tests |

## Fakes vs Mocks

| API Type | Recommendation |
|----------|----------------|
| Public/shared interfaces | Prefer fakes (test doubles implementing the interface) — tests behavior, not implementation |
| `internal` classes | Mocks (MockK) acceptable — simpler setup, internal APIs can change |
| Android framework (Context, etc.) | Use real Robolectric context unless you need `verify()` |

## Robolectric

Add `@RunWith(RobolectricTestRunner::class)` when your test needs Android `Context` (e.g., `RuntimeEnvironment.getApplication()`). Pure Kotlin logic tests (e.g., `FpsCalculatorTest`) do not need Robolectric.

## Assertions

Use Google Truth for assertions:

```kotlin
import com.google.common.truth.Truth.assertThat

assertThat(result).isEqualTo(expected)
assertThat(list).containsExactly(a, b, c).inOrder()
```
