package com.ms_square.debugoverlay.modules;

import android.view.View;
import android.view.ViewGroup;

public class FpsViewModule extends BaseViewModule<Double> {

    public FpsViewModule() {
        super(0);
    }

    public FpsViewModule(int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(Double data) {

    }

    @Override
    public View createView(ViewGroup root, int textColor, float textSize, float textAlpha) {
        return null;
    }
}
