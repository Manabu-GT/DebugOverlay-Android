# API Compatibility

## Current Status: Temporarily Disabled

API compatibility validation is **temporarily disabled** due to an incompatibility between AGP 9's built-in Kotlin support and the [Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator) (BCV) plugin.

### What happened

AGP 9 introduced built-in Kotlin compilation support and actively rejects the `org.jetbrains.kotlin.android` plugin. BCV hooks into that plugin to register its `apiCheck`/`apiDump` tasks. Without it, BCV applies silently but registers no tasks — API validation was broken since the AGP 9 upgrade with no visible error.

The intended replacement — Kotlin Gradle Plugin's built-in `abiValidation` — has the same limitation: it requires `KotlinAndroidProjectExtension` which AGP 9's built-in Kotlin does not provide.

### Upstream issues

- [Kotlin/binary-compatibility-validator#312](https://github.com/Kotlin/binary-compatibility-validator/issues/312) — BCV tasks not registered with AGP 9 (open, no maintainer response)
- [KT-81117](https://youtrack.jetbrains.com/issue/KT-81117) — KGP conflicts with AGP 9 built-in Kotlin
- BCV is in [maintenance mode](https://github.com/Kotlin/binary-compatibility-validator#readme), superseded by KGP's `abiValidation` (experimental since Kotlin 2.1.20)

### Workarounds considered

| Approach | Status |
|----------|--------|
| `android.builtInKotlin=false` | Works but temporary — removed in AGP 10 |
| Manual BCV task registration ([OkHttp approach](https://github.com/square/okhttp/pull/9375)) | KMP-specific, fragile |
| Fork BCV and fix | Maintaining a fork of a deprecated plugin is not worthwhile |
| KGP `abiValidation` | Requires `kotlin-android` plugin which AGP 9 rejects |

### Plan

1. Keep the existing `.api` files as a historical reference of the last validated public API surface
2. Monitor upstream issues for a fix in BCV, KGP, or AGP
3. Re-enable validation when the ecosystem supports AGP 9

---

## Why It Matters

Binary compatibility ensures that apps compiled against an older version of the library continue to work when upgraded to a newer version without recompilation. Breaking binary compatibility can cause:

- `NoSuchMethodError` at runtime
- `NoClassDefFoundError` for removed classes
- `IncompatibleClassChangeError` for changed class hierarchies

## The `.api` Files

Each published module has an `api/<module-name>.api` file that contains a text dump of its public API:

```text
debugoverlay/api/debugoverlay.api
debugoverlay-core/api/debugoverlay-core.api
debugoverlay-extension-okhttp/api/debugoverlay-extension-okhttp.api
debugoverlay-extension-timber/api/debugoverlay-extension-timber.api
```

These files are checked into version control. While validation is disabled, they serve as a reference but are not automatically checked against the current code.

## Handling Breaking Changes

### Adding New API (Non-breaking)

Adding new public classes or methods is safe and backward compatible.

**Note:** Adding new abstract properties/methods to interfaces is a breaking change for
existing implementers (they must implement the new member). However, since DebugOverlay
provides concrete implementations (e.g., `DebugOverlayTimberTree` for `LogSource`),
consumers typically don't implement these interfaces directly.

### Removing/Changing API (Breaking)

Breaking changes require careful consideration:

1. **Deprecation first** - Mark as `@Deprecated` with `ReplaceWith` suggestion
2. **Document in CHANGELOG** - Note the deprecation and migration path
3. **Version bump** - Follow semantic versioning
4. **Removal timeline** - Keep deprecated API for at least one minor version before removal

### Excluding Internal APIs

Use `@InternalDebugOverlayApi` annotation to mark APIs that are public in bytecode but not intended for external use:

```kotlin
@InternalDebugOverlayApi
public class SomeInternalHelper { ... }
```
