package com.ms_square.debugoverlay.modules;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.ViewModule;

import java.util.List;

public class CpuFreqModule extends OverlayModule<List<CpuFreq>> {

    public static final int DEFAULT_INTERVAL = 2000; // 2000ms

    public CpuFreqModule() {
        super(null, null);
    }

    public CpuFreqModule(int interval) {
        super(null, null);
    }

    public CpuFreqModule(int interval, int layoutResId) {
        super(null, null);
    }

    public CpuFreqModule(ViewModule<List<CpuFreq>> viewModule) {
        super(null, null);
    }

    public CpuFreqModule(int interval, ViewModule<List<CpuFreq>> viewModule) {
        super(null, null);
    }
}
