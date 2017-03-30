package com.ms_square.debugoverlay.sample;

import android.app.Application;
import android.graphics.Color;

import com.ms_square.debugoverlay.DebugOverlay;
import com.squareup.leakcanary.LeakCanary;

public class DebugOverlaySampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        // Simplest way to use
        //DebugOverlay.with(this).install();

        // Customize
        new DebugOverlay.Builder(this)
                .allowSystemLayer(true)
                .bgColor(Color.parseColor("#64000000"))
                .textColor(Color.WHITE)
                .build()
                .install();

        // Enable debug logging of DebugOverlay for its development.
        // You do not need to enable this if you use this library without any further customization.
        //DebugOverlay.enableDebugLogging(true);
    }
}
