package com.ms_square.debugoverlay.modules;

import android.view.View;
import android.view.ViewGroup;

public class SimpleViewModule extends BaseViewModule<String> {

    public SimpleViewModule(int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(String data) {

    }

    @Override
    public View createView(ViewGroup root, int textColor, float textSize, float textAlpha) {
        return null;
    }
}
