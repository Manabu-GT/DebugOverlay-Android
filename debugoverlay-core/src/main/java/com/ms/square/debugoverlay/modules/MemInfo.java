package com.ms.square.debugoverlay.modules;

import android.app.ActivityManager;
import android.os.Debug;

public class MemInfo {

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
