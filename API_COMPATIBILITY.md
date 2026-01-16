# API Compatibility

This project uses [Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator) (BCV) to track public API changes and prevent accidental breaking changes.

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

These files are checked into version control and serve as the "golden" reference for the public API.

## Gradle Tasks

| Task | Purpose |
|------|---------|
| `./gradlew apiCheck` | Compares current API against `.api` files. Fails if there are differences. Runs automatically as part of `check`. |
| `./gradlew apiDump` | Regenerates `.api` files from current code. Use after making intentional API changes. |

### Module-specific tasks

```bash
# Check a specific module
./gradlew :debugoverlay-core:apiCheck

# Update a specific module's API dump
./gradlew :debugoverlay-core:apiDump
```

## Workflow

### Making API Changes

1. **Make your changes** to the public API (add/modify/remove public classes, methods, etc.)

2. **Run `apiCheck`** to see what changed:
   ```bash
   ./gradlew apiCheck
   ```
   This will fail and show the differences.

3. **Review the diff** to ensure changes are intentional:
   - Adding new public API is generally safe
   - Removing or modifying existing API is a breaking change

4. **Update the API dump** if changes are intentional:
   ```bash
   ./gradlew apiDump
   ```

5. **Commit both** your code changes and the updated `.api` files together.

### CI Failures

If CI fails on `apiCheck`:

1. **Accidental change?** - Revert unintended public API exposure (e.g., missing `internal` modifier)

2. **Intentional change?** - Run `apiDump` locally and commit the updated `.api` files

3. **Merge conflict in `.api` files?** - Regenerate with `apiDump` after resolving code conflicts

## Handling Breaking Changes

### Adding New API (Non-breaking)

Adding new public classes, methods, or properties is safe and backward compatible:

```kotlin
// Before
public interface LogSource {
    val logs: Flow<List<LogEntry>>
}

// After - new property added (safe)
public interface LogSource {
    val logs: Flow<List<LogEntry>>
    val sourceName: String  // New - consumers don't need to implement this if using classes
}
```

### Removing/Changing API (Breaking)

Breaking changes require careful consideration:

1. **Deprecation first** - Mark as `@Deprecated` with `ReplaceWith` suggestion:
   ```kotlin
   @Deprecated(
       message = "Use newMethod() instead",
       replaceWith = ReplaceWith("newMethod()"),
       level = DeprecationLevel.WARNING
   )
   public fun oldMethod() { ... }
   ```

2. **Document in CHANGELOG** - Note the deprecation and migration path

3. **Version bump** - Follow semantic versioning:
   - Patch (1.0.x): Bug fixes only, no API changes
   - Minor (1.x.0): New features, deprecations allowed, no removals
   - Major (x.0.0): Breaking changes, removals allowed

4. **Removal timeline** - Keep deprecated API for at least one minor version before removal

### Example: Safe Deprecation Cycle

```text
v2.0.0 - Original API
v2.1.0 - Add new API, deprecate old API (WARNING level)
v2.2.0 - Change deprecation to ERROR level
v3.0.0 - Remove deprecated API
```

## Configuration

BCV is configured in each module's `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.bcv)
}

apiValidation {
    // Exclude classes annotated with this from API tracking
    nonPublicMarkers.add("com.ms.square.debugoverlay.internal.InternalDebugOverlayApi")
}
```

### Excluding Internal APIs

Use `@InternalDebugOverlayApi` annotation to mark APIs that are public in bytecode but not intended for external use:

```kotlin
@InternalDebugOverlayApi
public class SomeInternalHelper { ... }
```

## Common Scenarios

### "I added a new public method but didn't mean to"

Make the method `internal` or `private`, then verify with `apiCheck`.

### "I need to expose internal classes temporarily"

Don't. Instead, design a proper public API or use `@InternalDebugOverlayApi` to signal "use at your own risk."

### "The API dump shows generated classes like `ComposableSingletons$*`"

In debugoverlay-core, these are filtered out via `apiValidation.ignoredPackages` to reduce noise.
If you see them in other modules, they're Compose compiler artifacts and don't affect consumers.

### "Merge conflicts in `.api` files"

Don't manually resolve - just run `apiDump` after resolving code conflicts.
