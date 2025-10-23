package com.ms_square.debugoverlay.sample;

import android.app.Application;

import com.ms_square.debugoverlay.DebugOverlay;

public class DebugOverlaySampleApplication extends Application {

    private static final String TAG = DebugOverlaySampleApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        // Simplest way to use
        DebugOverlay.with(this).install();

        // Fully Customize
//        new DebugOverlay.Builder(this)
//                .modules(new CpuUsageModule(),
//                        new MemInfoModule(this),
//                        new FpsModule(),
//                        new LogcatModule())
//                .position(Position.BOTTOM_START)
//                .bgColor(Color.parseColor("#60000000"))
//                .textColor(Color.MAGENTA)
//                .textSize(14f)
//                .textAlpha(.8f)
//                .allowSystemLayer(true)
//                .notification(true, MainActivity.class.getName())
//                .build()
//                .install();

        // Use custom module
//        new DebugOverlay.Builder(this)
//                .modules(new CpuFreqModule(),
//                        new CpuUsageModule(),
//                        new MemInfoModule(this),
//                        new FpsModule(),
//                        new IPAddressModule(this),
//                        new NetStatsModule(),
//                        new TimberModule(BuildConfig.DEBUG))
//                .build()
//                .install();

        // Enable debug logging of DebugOverlay for its development.
        // You do not need to enable this if you use this library without any further customization.
        //DebugOverlay.enableDebugLogging(true);
    }
}
