package com.ms_square.debugoverlay.modules;

import android.app.ActivityManager;
import android.os.Debug;

public class MemInfo {

    public MemInfo(ActivityManager.MemoryInfo systemMemInfo,
                   Debug.MemoryInfo processMemInfo) {

    }

    public ActivityManager.MemoryInfo getSystemMemInfo() {
        return null;
    }

    public Debug.MemoryInfo getProcessMemInfo() {
        return null;
    }
}