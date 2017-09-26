Transient Info Extension Module
===============================

**TransientInfoModule** is an extension module which allows custom data to be
added and remove from the overlay while the application is running.
This is useful when certain types of information is only relevant in specific
parts of the application.

<img src="../art/overlay_with_transient_info_module_small.png" width="50%" alt="DebugOverlay Screen Capture">

Setup
-----

Gradle:

```groovy
dependencies {
  debugCompile 'com.ms-square:debugoverlay:1.1.3'
  releaseCompile 'com.ms-square:debugoverlay-no-op:1.1.3'
  testCompile 'com.ms-square:debugoverlay-no-op:1.1.3'

  compile ('com.ms-square:debugoverlay-ext-transient-info:1.1.3') {
    exclude module: 'debugoverlay'
  }
}
```

or

```groovy
dependencies {
  // this will use a full debugoverlay lib even in the test/release build
  compile 'com.ms-square:debugoverlay-ext-transient-info:1.1.3'
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
                     new TransientInfoModule(1000))
            .build()
            .install();
    // Normal app init code...
  }
}
```

In your `Activity` class where you want to monitor something:

```java
public class ExampleActivity extends Activity {

  private final MyVideoPlayer videoPlayer = new MyVideoPlayer();

  @Override public void onCreate() {
    super.onCreate();
    TransientInfoModule.addProvider(new InfoProvider() {
      @Override String getInfo()
      {
        return "Frame drops: " + videoPlayer.frameDrops();
      }
    });
    // Normal activity onCreate code...
  }

  @Override public void onDestroy() {
    super.onCreate();
    TransientInfoModule.clearProviders();
    // Normal activity onDestroy code...
  }
}
```
