# Change Log

## Version 1.1.4 *(2025-10-27)*

* **Build & toolchain** – Migrated the entire project to Gradle Kotlin DSL with a shared `libs.versions.toml`, upgraded the wrapper/AGP stack (Gradle 8.13, AGP 8.13.0).
* **Android platform updates** – Raised `minSdk` to 26 / `targetSdk` to 36, migrated dependencies to AndroidX, and cleaned up manifest/service initialization (DebugOverlay now installs only in the app’s main process and drops stale permission checks).
* **CPU/FPS modules** – Fixed CPU usage/frequency collectors to operate correctly on API 26+ and tidied related overlays/resources.

## Version 1.1.3 *(2017-09-24)*

* Add a NetStatsModule as an extension module
* Support Library 26.0.1 -> 26.1.0

## Version 1.1.2 *(2017-09-09)*

* Add a TimberModule as an extension module
* Update the LogcatLine to make it work with the TimberModule

## Version 1.1.1 *(2017-09-02)*

* Fix to close filereaders in CpuFreqDataModule after reading data

## Version 1.1.0 *(2017-09-01)*

* Android O support (Note: CpuUsageModule/CpuFreqModule will not work on Android O and above)
* Add a CpuFreqModule to show current/max frequencies of all the cpu cores
* Support Library 25.3.1 -> 26.0.1

## Version 1.0.1 *(2017-04-05)*

* Use split v4 support library as dependencies for less size
* Support Library 25.3.0 -> 25.3.1

## Version 1.0.0 *(2017-04-04)*

Initial release.
