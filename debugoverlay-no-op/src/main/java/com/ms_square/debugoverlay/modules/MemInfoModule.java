package com.ms_square.debugoverlay.modules;

import android.content.Context;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.ViewModule;

public class MemInfoModule extends OverlayModule<MemInfo> {

    public static final int DEFAULT_INTERVAL = 1500; // 1500ms

    public MemInfoModule(Context context) {
        super(null, null);
    }

    public MemInfoModule(Context context, int interval) {
        super(null, null);
    }

    public MemInfoModule(Context context, int interval, int layoutResId) {
        super(null, null);
    }

    public MemInfoModule(Context context, ViewModule<MemInfo> viewModule) {
        super(null, null);
    }

    public MemInfoModule(Context context, int interval, ViewModule<MemInfo> viewModule) {
        super(null, null);
    }
}
