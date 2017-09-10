package com.ms_square.debugoverlay.modules;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.NonNull;

class MemInfoDataModule extends BaseDataModule<MemInfo> {

    private Handler handler = new Handler(Looper.getMainLooper());

    private final ActivityManager am;

    private MemInfo memInfo;

    private final int interval;

    public MemInfoDataModule(@NonNull Context context, int interval) {
        this.interval = interval;
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
    protected MemInfo getLatestData() {
        return memInfo;
    }

    private final Runnable memInfoQueryRunnable = new Runnable() {
        @Override
        public void run() {
            ActivityManager.MemoryInfo systemMemInfo = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(systemMemInfo);
            Debug.MemoryInfo processMemInfo = am.getProcessMemoryInfo(new int[]{Process.myPid()})[0];
            memInfo = new MemInfo(systemMemInfo, processMemInfo);
            notifyObservers();
            handler.postDelayed(memInfoQueryRunnable, interval);
        }
    };
}
