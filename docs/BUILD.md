# Build & Gradle

## Java Toolchain

All modules target **JDK 21**. Build scripts include:

```kotlin
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

## Version Catalog

- Versions are centralized in `gradle/libs.versions.toml`
- Gradle wrapper version in `gradle/wrapper/gradle-wrapper.properties`
- Repository definitions in `settings.gradle.kts`
- Avoid deprecated repositories (e.g., JCenter)

## Module Conventions

- Use `namespace` declarations in library modules
- All modules use AndroidX (no legacy `android.support`)
- Keep resource prefix: `resourcePrefix 'debugoverlay_'`

## Project Modules

| Module | Description |
|--------|-------------|
| `debugoverlay-core` | Compose-based runtime and shared components |
| `debugoverlay` | Primary public API artifact |
| `debugoverlay-extension-okhttp` | OkHttp interceptor for network tracking |
| `debugoverlay-extension-timber` | Timber tree for log capture |
| `sample` | Demo application |

## Resource Handling

- New assets go in the correct variant directory
- Include mdpi/hdpi/etc. density variants when required
