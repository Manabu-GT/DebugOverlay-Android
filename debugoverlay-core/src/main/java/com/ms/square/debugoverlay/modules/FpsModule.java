package com.ms.square.debugoverlay.modules;


import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.ms.square.debugoverlay.OverlayModule;
import com.ms.square.debugoverlay.ViewModule;

public class FpsModule extends OverlayModule<Double> {

    public static final int DEFAULT_INTERVAL = 1000; // 1000ms

    public FpsModule() {
        super(new FpsDataModule(DEFAULT_INTERVAL), new FpsViewModule());
    }

    public FpsModule(int interval) {
        super(new FpsDataModule(interval), new FpsViewModule());
    }

    public FpsModule(int interval, @LayoutRes int layoutResId) {
        super(new FpsDataModule(interval), new FpsViewModule(layoutResId));
    }

    public FpsModule(@NonNull ViewModule<Double> viewModule) {
        super(new FpsDataModule(DEFAULT_INTERVAL), viewModule);
    }

    public FpsModule(int interval, @NonNull ViewModule<Double> viewModule) {
        super(new FpsDataModule(interval), viewModule);
    }
}
