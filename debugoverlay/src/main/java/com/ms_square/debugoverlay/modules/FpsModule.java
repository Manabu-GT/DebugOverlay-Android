package com.ms_square.debugoverlay.modules;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.ViewModule;

public class FpsModule extends OverlayModule<Double> {

    public FpsModule() {
        super(new FpsDataModule(), new FpsViewModule());
    }

    public FpsModule(int interval) {
        super(new FpsDataModule(interval), new FpsViewModule());
    }

    public FpsModule(int interval, @LayoutRes int layoutResId) {
        super(new FpsDataModule(interval), new FpsViewModule(layoutResId));
    }

    public FpsModule(@NonNull ViewModule<Double> viewModule) {
        super(new FpsDataModule(), viewModule);
    }

    public FpsModule(int interval, @NonNull ViewModule<Double> viewModule) {
        super(new FpsDataModule(interval), viewModule);
    }
}
