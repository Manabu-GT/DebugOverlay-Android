package com.ms_square.debugoverlay.sample;

import android.app.Application;

import com.ms_square.debugoverlay.DebugOverlay;
import com.squareup.leakcanary.LeakCanary;

public class DebugOverlaySampleApplication extends Application {

    private static final String TAG = DebugOverlaySampleApplication.class.getSimpleName();

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

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Log.v(TAG, "hello world V");
//                Log.d(TAG, "hello world D");
//
//                Log.i(TAG, "hello world I");
//                Log.w(TAG, "hello world W");
//
//                Log.e(TAG, "hello world E");
//                Log.wtf(TAG, "hello world WTF");
//            }
//        }, 1000);

        // Enable debug logging of DebugOverlay for its development.
        // You do not need to enable this if you use this library without any further customization.
        //DebugOverlay.enableDebugLogging(true);
    }
}
