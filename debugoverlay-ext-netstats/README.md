Network Statistics Extension Module
===================================

**NetStatsModule** is an extension module which shows the total network usage of the application.
The stats include all network interfaces, and both TCP and UDP usage.

<img src="../art/overlay_with_netstats_module_small.png" width="50%" alt="DebugOverlay Screen Capture">

Setup
-----

Gradle:

```groovy
dependencies {
  debugCompile 'com.ms-square:debugoverlay:1.1.3'
  releaseCompile 'com.ms-square:debugoverlay-no-op:1.1.3'
  testCompile 'com.ms-square:debugoverlay-no-op:1.1.3'

  compile ('com.ms-square:debugoverlay-ext-netstats:1.1.3') {
    exclude module: 'debugoverlay'
  }
}
```

or

```groovy
dependencies {
  // this will use a full debugoverlay lib even in the test/release build
  compile 'com.ms-square:debugoverlay-ext-netstats:1.1.3'
}
```

Usage
-----

### Simple Example

In your `Application` class:

```java
public class ExampleApplication extends Application {

  @Override public void onCreate() {
    super.onCreate();
    new DebugOverlay.Builder(this)
            .modules(new CpuUsageModule(),
                     new MemInfoModule(this),
                     new FpsModule(),
                     new NetStatsModule())
            .build()
            .install();
    // Normal app init code...
  }
}
```
