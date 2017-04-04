package com.ms_square.debugoverlay.modules;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.ViewModule;

public class CpuUsageModule extends OverlayModule<CpuUsage> {

    public static final int DEFAULT_INTERVAL = 1000; // 1000ms

    public CpuUsageModule() {
        super(null, null);
    }

    public CpuUsageModule(int interval) {
        super(null, null);
    }

    public CpuUsageModule(int interval, int layoutResId) {
        super(null, null);
    }

    public CpuUsageModule(ViewModule<CpuUsage> viewModule) {
        super(null, null);
    }

    public CpuUsageModule(int interval, ViewModule<CpuUsage> viewModule) {
        super(null, null);
    }
}