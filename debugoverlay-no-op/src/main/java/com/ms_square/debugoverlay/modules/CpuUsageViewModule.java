package com.ms_square.debugoverlay.modules;

import android.view.View;
import android.view.ViewGroup;

public class CpuUsageViewModule extends BaseViewModule<CpuUsage> {

    public CpuUsageViewModule() {
        super(0);
    }

    public CpuUsageViewModule(int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(CpuUsage data) {

    }

    @Override
    public View createView(ViewGroup root, int textColor, float textSize, float textAlpha) {
        return null;
    }
}
