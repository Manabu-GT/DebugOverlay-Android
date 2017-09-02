package com.ms_square.debugoverlay.modules;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class CpuFreqViewModule extends BaseViewModule<List<CpuFreq>> {

    public CpuFreqViewModule() {
        super(0);
    }

    public CpuFreqViewModule(int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(List<CpuFreq> cpuFreqList) {
    }

    @Override
    public View createView(ViewGroup root, int textColor, float textSize, float textAlpha) {
        return null;
    }
}
