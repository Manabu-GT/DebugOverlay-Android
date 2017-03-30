package com.ms_square.debugoverlay.modules;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.NonNull;

import com.ms_square.debugoverlay.OverlayViewDelegateFactory;

public class MemInfoModule extends BaseOverlayModule<MemInfoModule.MemInfo> {

    public static final int DEFAULT_INTERVAL = 1500; // 1500ms

    private Handler handler = new Handler(Looper.getMainLooper());

    private final ActivityManager am;

    private MemInfo memInfo;

    public MemInfoModule(@NonNull Context context) {
        this(context, DEFAULT_INTERVAL);
    }

    public MemInfoModule(@NonNull Context context, int interval) {
        super(interval);
        am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public MemInfoModule(@NonNull Context context, @NonNull OverlayViewDelegateFactory<MemInfo> viewDelegateFactory) {
        this(context, DEFAULT_INTERVAL, viewDelegateFactory);
    }

    public MemInfoModule(@NonNull Context context, int interval, @NonNull OverlayViewDelegateFactory<MemInfo> viewDelegateFactory) {
        super(interval, viewDelegateFactory);
        am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    @Override
    public void start() {
        handler.post(memInfoQueryRunnable);
    }

    @Override
    public void stop() {
        handler.removeCallbacks(memInfoQueryRunnable);
    }

    @Override
    protected OverlayViewDelegateFactory<MemInfo> getDefaultFactory() {
        return new MemInfoViewDelegate.Factory();
    }

    @Override
    protected MemInfo getLatestData() {
        return memInfo;
    }

    final Runnable memInfoQueryRunnable = new Runnable() {
        @Override
        public void run() {
            ActivityManager.MemoryInfo systemMemInfo = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(systemMemInfo);
            Debug.MemoryInfo processMemInfo = am.getProcessMemoryInfo(new int[]{Process.myPid()})[0];
            memInfo = new MemInfo(systemMemInfo, processMemInfo);
            notifyObservers();
            handler.postDelayed(memInfoQueryRunnable, getInterval());
        }
    };

    public static class MemInfo {
        private final ActivityManager.MemoryInfo systemMemInfo;
        private final Debug.MemoryInfo processMemInfo;

        public MemInfo(ActivityManager.MemoryInfo systemMemInfo,
                        Debug.MemoryInfo processMemInfo) {
            this.systemMemInfo = systemMemInfo;
            this.processMemInfo = processMemInfo;
        }

        public ActivityManager.MemoryInfo getSystemMemInfo() {
            return systemMemInfo;
        }

        public Debug.MemoryInfo getProcessMemInfo() {
            return processMemInfo;
        }
    }
}
