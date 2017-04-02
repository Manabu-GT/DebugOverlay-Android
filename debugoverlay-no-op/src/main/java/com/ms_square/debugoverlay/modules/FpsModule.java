package com.ms_square.debugoverlay.modules;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.ViewModule;

public class FpsModule extends OverlayModule<Double> {

    public static final int DEFAULT_INTERVAL = 1000; // 1000ms

    public FpsModule() {
        super(null, null);
    }

    public FpsModule(int interval) {
        super(null, null);
    }

    public FpsModule(int interval, int layoutResId) {
        super(null, null);
    }

    public FpsModule(ViewModule<Double> viewModule) {
        super(null, null);
    }

    public FpsModule(int interval, ViewModule<Double> viewModule) {
        super(null, null);
    }
}
