package com.ms_square.debugoverlay.modules;

import android.view.View;
import android.view.ViewGroup;

public class MemInfoViewModule extends BaseViewModule<MemInfo> {

    public MemInfoViewModule() {
        super(0);
    }

    public MemInfoViewModule(int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(MemInfo data) {
    }

    @Override
    public View createView(ViewGroup root, int textColor, float textSize, float textAlpha) {
        return null;
    }
}
